package com.itzetech.dpdms.audit;

import com.itzetech.dpdms.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditRepository repo;

    public AuditService(AuditRepository repo) {
        this.repo = repo;
    }

    public void record(AuthUser actor, String action, String subject) {
        record(actor.id(), actor.fullName(), action, subject);
    }

    /** Records an action. Runs in the caller's transaction so the record and the change succeed or fail together. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(UUID actorId, String actorName, String action, String subject) {
        Instant now = Instant.now();
        repo.save(new AuditEntry(now, actorId, actorName, action, subject, reference(now, actorName, action, subject)));
    }

    private static String reference(Instant at, String actor, String action, String subject) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest((at + "|" + actor + "|" + action + "|" + subject).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
