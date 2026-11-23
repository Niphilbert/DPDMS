package com.itzetech.dpdms.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.common.PasswordPolicy;
import com.itzetech.dpdms.config.AppProperties;
import com.itzetech.dpdms.mail.MailService;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * What the administrator can do with accounts. Staff are added here (invited with a temporary password);
 * clients create their own accounts, but can still be deactivated or have their password reset here.
 */
@Service
@Transactional
public class UserAdminService {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserWithSecret(UserDto user, String temporaryPassword) {
    }

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final AuditService audit;
    private final AppProperties props;

    UserAdminService(UserAccountRepository users, PasswordEncoder encoder, MailService mail, AuditService audit, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.mail = mail;
        this.audit = audit;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list(Role role, String q) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return users.search(role, like).stream().map(UserDto::of).toList();
    }

    public UserWithSecret createStaff(AuthUser actor, String fullName, String rawEmail, Role role) {
        if (!role.isStaff()) {
            throw ApiException.badRequest("ROLE_NOT_STAFF", "Only Itzetech staff are added here. Clients create their own account.");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists.");
        }
        String temp = PasswordPolicy.temporary();
        UserAccount u = new UserAccount(email, fullName.trim(), encoder.encode(temp), role, UserStatus.INVITED);
        u.setMustChangePassword(true);
        u.setCreatedBy(actor.id());
        users.save(u);
        sendInvitation(u, temp);
        audit.record(actor, "User created", u.getFullName() + " · " + role.label());
        return withSecret(u, temp);
    }

    public UserDto changeRole(AuthUser actor, UUID id, Role role) {
        UserAccount u = get(id);
        notSelf(actor, u, "You cannot change your own role.");
        if (!u.getRole().isStaff() || !role.isStaff()) {
            throw ApiException.badRequest("ROLE_NOT_STAFF", "Only staff roles can be changed. Clients stay clients.");
        }
        if (u.getRole() == role) {
            return UserDto.of(u);
        }
        keepOneAdmin(u);
        Role old = u.getRole();
        u.setRole(role);
        audit.record(actor, "Role changed", u.getFullName() + ": " + old.label() + " to " + role.label());
        return UserDto.of(u);
    }

    public UserDto deactivate(AuthUser actor, UUID id) {
        UserAccount u = get(id);
        notSelf(actor, u, "You cannot deactivate your own account.");
        if (u.getStatus() != UserStatus.DEACTIVATED) {
            keepOneAdmin(u);
            u.setStatus(UserStatus.DEACTIVATED);
            audit.record(actor, "User deactivated", u.getFullName());
        }
        return UserDto.of(u);
    }

    public UserDto reactivate(AuthUser actor, UUID id) {
        UserAccount u = get(id);
        if (u.getStatus() == UserStatus.DEACTIVATED) {
            if (u.isMustChangePassword()) {
                u.setStatus(UserStatus.INVITED);
            } else if (u.getRole() == Role.CLIENT && u.getLastLoginAt() == null) {
                u.setStatus(UserStatus.PENDING_VERIFICATION);
            } else {
                u.setStatus(UserStatus.ACTIVE);
            }
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
            audit.record(actor, "User reactivated", u.getFullName());
        }
        return UserDto.of(u);
    }

    /** Gives the person a new temporary password; they must choose their own at the next sign-in. */
    public UserWithSecret resetPassword(AuthUser actor, UUID id) {
        UserAccount u = get(id);
        notSelf(actor, u, "Change your own password from your profile instead.");
        if (u.getStatus() == UserStatus.PENDING_VERIFICATION || u.getStatus() == UserStatus.DEACTIVATED) {
            throw ApiException.badRequest("CANNOT_RESET", "This account is not active. Reactivate it first.");
        }
        String temp = PasswordPolicy.temporary();
        u.setPasswordHash(encoder.encode(temp));
        u.setMustChangePassword(true);
        u.setStatus(UserStatus.INVITED);
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        sendInvitation(u, temp);
        audit.record(actor, "Password reset", u.getFullName());
        return withSecret(u, temp);
    }

    // ---- helpers ----

    private UserAccount get(UUID id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
    }

    private static void notSelf(AuthUser actor, UserAccount target, String message) {
        if (actor.id().equals(target.getId())) {
            throw ApiException.badRequest("SELF_NOT_ALLOWED", message);
        }
    }

    /** The system must always keep at least one administrator who can sign in. */
    private void keepOneAdmin(UserAccount target) {
        if (target.getRole() == Role.ADMIN && target.isActive() && users.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw ApiException.badRequest("LAST_ADMIN", "There must be at least one active administrator.");
        }
    }

    private UserWithSecret withSecret(UserAccount u, String temp) {
        return new UserWithSecret(UserDto.of(u), props.exposeSecrets() ? temp : null);
    }

    private void sendInvitation(UserAccount u, String temp) {
        mail.send(u.getEmail(), "Your Itzetech account",
                "Hello " + u.getFullName() + ",\n\nAn account was created for you as " + u.getRole().label() + ".\n"
                        + "Sign in at " + props.frontendUrl() + "/login with this temporary password:\n\n    " + temp
                        + "\n\nYou will be asked to choose a new password the first time you sign in.\n");
    }
}
