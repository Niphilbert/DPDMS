package com.itzetech.dpdms.auth;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.common.PasswordPolicy;
import com.itzetech.dpdms.config.AppProperties;
import com.itzetech.dpdms.mail.MailService;
import com.itzetech.dpdms.notification.NotificationService;
import com.itzetech.dpdms.security.TokenService;
import com.itzetech.dpdms.user.ClientCompany;
import com.itzetech.dpdms.user.ClientCompanyRepository;
import com.itzetech.dpdms.user.Role;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import com.itzetech.dpdms.user.UserDto;
import com.itzetech.dpdms.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static com.itzetech.dpdms.auth.AuthDtos.AuthResult;
import static com.itzetech.dpdms.auth.AuthDtos.RegisterRequest;

/**
 * Sign-in in steps: password, then (for staff invited by the administrator) a new password,
 * then (for administrators) an e-mailed code. Clients register themselves and confirm their e-mail address with a code.
 * Failed attempts are saved even though the request ends in an error, hence noRollbackFor.
 */
@Service
@Transactional(noRollbackFor = ApiException.class)
class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);
    private static final Duration STEP_TIME = Duration.ofMinutes(15);

    private final UserAccountRepository users;
    private final ClientCompanyRepository companies;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final OtpService otp;
    private final MailService mail;
    private final AppProperties props;
    private final AuditService audit;
    private final NotificationService notifications;
    private final String dummyHash;

    AuthService(UserAccountRepository users, ClientCompanyRepository companies, PasswordEncoder encoder, TokenService tokens,
                OtpService otp, MailService mail, AppProperties props, AuditService audit, NotificationService notifications) {
        this.users = users;
        this.companies = companies;
        this.encoder = encoder;
        this.tokens = tokens;
        this.otp = otp;
        this.mail = mail;
        this.props = props;
        this.audit = audit;
        this.notifications = notifications;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    AuthResult login(String rawEmail, String password) {
        UserAccount u = users.findByEmail(normalize(rawEmail)).orElse(null);
        if (u == null) {
            encoder.matches(password, dummyHash); // same time as a real check, so the response does not reveal whether the email exists
            throw badCredentials();
        }
        Instant now = Instant.now();
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                    "Too many wrong passwords. Try again in a few minutes.");
        }
        if (!encoder.matches(password, u.getPasswordHash())) {
            int failed = u.getFailedAttempts() + 1;
            if (failed >= MAX_FAILED_ATTEMPTS) {
                u.setLockedUntil(now.plus(LOCK_TIME));
                failed = 0;
            }
            u.setFailedAttempts(failed);
            throw badCredentials();
        }
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        return switch (u.getStatus()) {
            case DEACTIVATED -> throw ApiException.forbidden("ACCOUNT_DEACTIVATED",
                    "This account is deactivated. Ask the administrator to reactivate it.");
            case PENDING_VERIFICATION -> throw ApiException.forbidden("EMAIL_NOT_VERIFIED",
                    "Please confirm your email address first. We can send you a new code.");
            case INVITED -> AuthResult.passwordChangeRequired(tokens.issueChallenge(u.getId(), TokenService.CHANGE_PASSWORD, STEP_TIME));
            case ACTIVE -> afterPassword(u);
        };
    }

    AuthResult verifySignInCode(String challenge, String code) {
        UserAccount u = requireUser(tokens.readChallenge(challenge, TokenService.MFA));
        otp.verify(u.getId(), OneTimeCode.Purpose.ADMIN_SIGN_IN, code);
        return complete(u);
    }

    AuthResult changePassword(String challenge, String newPassword) {
        UserAccount u = requireUser(tokens.readChallenge(challenge, TokenService.CHANGE_PASSWORD));
        if (u.getStatus() != UserStatus.INVITED) {
            throw ApiException.badRequest("NOT_INVITED", "This account does not need a new password.");
        }
        PasswordPolicy.require(newPassword);
        if (encoder.matches(newPassword, u.getPasswordHash())) {
            throw ApiException.badRequest("SAME_PASSWORD", "Choose a different password from the temporary one.");
        }
        u.setPasswordHash(encoder.encode(newPassword));
        u.setStatus(UserStatus.ACTIVE);
        u.setMustChangePassword(false);
        audit.record(u.getId(), u.getFullName(), "Password changed", u.getEmail());
        return afterPassword(u);
    }

    AuthResult register(RegisterRequest r) {
        PasswordPolicy.require(r.password());
        String email = normalize(r.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists. Sign in instead.");
        }
        ClientCompany company = companies.save(new ClientCompany(r.companyName().trim(), r.fullName().trim(),
                r.phone() == null || r.phone().isBlank() ? null : r.phone().trim()));
        UserAccount u = new UserAccount(email, r.fullName().trim(), encoder.encode(r.password()), Role.CLIENT, UserStatus.PENDING_VERIFICATION);
        u.setCompany(company);
        users.save(u);
        return sendVerification(u);
    }

    AuthResult verifyEmail(String rawEmail, String code) {
        UserAccount u = users.findByEmail(normalize(rawEmail))
                .orElseThrow(() -> ApiException.badRequest("CODE_INVALID", "That code is not right."));
        if (u.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw ApiException.badRequest("ALREADY_VERIFIED", "This email address is already confirmed. Sign in instead.");
        }
        otp.verify(u.getId(), OneTimeCode.Purpose.EMAIL_VERIFICATION, code);
        u.setStatus(UserStatus.ACTIVE);
        audit.record(u.getId(), u.getFullName(), "Client registered", u.getCompany().getName());
        notifications.notifyRole(Role.ADMIN, "New client registered: " + u.getCompany().getName() + " (" + u.getFullName() + ").", "/admin/users");
        return complete(u);
    }

    /**
     * Sends a new code to a client who has not confirmed yet. Says nothing about whether the address is known.
     * Returns the code only when secrets are exposed (development), so the dev UI can show it; otherwise null.
     */
    String resendVerification(String rawEmail) {
        return users.findByEmail(normalize(rawEmail))
                .filter(u -> u.getStatus() == UserStatus.PENDING_VERIFICATION)
                .map(this::sendVerification)
                .map(AuthResult::devCode)
                .orElse(null);
    }

    UserDto me(UUID userId) {
        return UserDto.of(requireUser(userId));
    }

    // ---- helpers ----

    private AuthResult sendVerification(UserAccount u) {
        String code = otp.issue(u.getId(), OneTimeCode.Purpose.EMAIL_VERIFICATION);
        mail.send(u.getEmail(), "Confirm your email address",
                "Hello " + u.getFullName() + ",\n\nYour Itzetech confirmation code is: " + code
                        + "\n\nIt works for 10 minutes. If you did not create an account, ignore this message.\n");
        return AuthResult.verificationRequired(u.getEmail(), props.exposeSecrets() ? code : null);
    }

    private AuthResult afterPassword(UserAccount u) {
        if (u.getRole() == Role.ADMIN) {
            String code = otp.issue(u.getId(), OneTimeCode.Purpose.ADMIN_SIGN_IN);
            mail.send(u.getEmail(), "Your sign-in code",
                    "Hello " + u.getFullName() + ",\n\nYour Itzetech sign-in code is: " + code
                            + "\n\nIt works for 10 minutes. If you did not try to sign in, tell another administrator.\n");
            return AuthResult.codeRequired(tokens.issueChallenge(u.getId(), TokenService.MFA, STEP_TIME), props.exposeSecrets() ? code : null);
        }
        return complete(u);
    }

    private AuthResult complete(UserAccount u) {
        u.setLastLoginAt(Instant.now());
        audit.record(u.getId(), u.getFullName(), "Signed in", u.getEmail());
        return AuthResult.authenticated(tokens.issueAccess(u), UserDto.of(u));
    }

    private UserAccount requireUser(UUID id) {
        return users.findById(id).orElseThrow(() -> ApiException.unauthorized("CHALLENGE_INVALID", "This step has expired. Please sign in again."));
    }

    private static ApiException badCredentials() {
        return ApiException.unauthorized("BAD_CREDENTIALS", "The email or password is incorrect.");
    }
}
