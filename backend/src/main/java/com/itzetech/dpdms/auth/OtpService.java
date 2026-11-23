package com.itzetech.dpdms.auth;

import com.itzetech.dpdms.common.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Six-digit codes sent by e-mail: to confirm a client's address and as the administrator's second step at sign-in. */
@Service
class OtpService {

    static final Duration LIFETIME = Duration.ofMinutes(10);
    static final Duration RESEND_AFTER = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OneTimeCodeRepository repo;
    private final PasswordEncoder encoder;

    OtpService(OneTimeCodeRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    /**
     * Creates a fresh code (older ones stop working) and returns it so the caller can e-mail it.
     * The "too soon" refusal happens before anything is written, so it must not mark the caller's
     * transaction rollback-only (that turned a clean 429 into a 500).
     */
    @Transactional(noRollbackFor = ApiException.class)
    String issue(UUID userId, OneTimeCode.Purpose purpose) {
        repo.findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId, purpose)
                .filter(c -> c.getCreatedAt().plus(RESEND_AFTER).isAfter(Instant.now()))
                .ifPresent(c -> {
                    throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "CODE_TOO_SOON",
                            "A code was just sent. Please wait a minute before asking for another.");
                });
        repo.deleteFor(userId, purpose);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        repo.save(new OneTimeCode(userId, purpose, encoder.encode(code), Instant.now().plus(LIFETIME)));
        return code;
    }

    /** Checks a code. A wrong code counts as an attempt; after five the code is dead and a new one must be requested. */
    @Transactional(noRollbackFor = ApiException.class)
    void verify(UUID userId, OneTimeCode.Purpose purpose, String code) {
        OneTimeCode c = repo.findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId, purpose)
                .orElseThrow(() -> ApiException.badRequest("CODE_INVALID", "That code is not right. Ask for a new one."));
        if (c.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("CODE_EXPIRED", "That code has expired. Ask for a new one.");
        }
        if (c.getAttempts() >= MAX_ATTEMPTS) {
            throw ApiException.badRequest("CODE_LOCKED", "Too many wrong attempts. Ask for a new code.");
        }
        c.addAttempt();
        if (code == null || !encoder.matches(code.trim(), c.getCodeHash())) {
            throw ApiException.badRequest("CODE_INVALID", "That code is not right.");
        }
        c.markUsed();
    }
}
