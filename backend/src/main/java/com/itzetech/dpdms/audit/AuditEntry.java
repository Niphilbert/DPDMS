package com.itzetech.dpdms.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One line of the audit trail: who did what, and when. Entries are only ever added. */
@Entity
@Table(name = "audit_entry")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_name", nullable = false, length = 150)
    private String actorName;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 300)
    private String subject;

    @Column(nullable = false, length = 12)
    private String ref;

    protected AuditEntry() {
    }

    AuditEntry(Instant occurredAt, UUID actorUserId, String actorName, String action, String subject, String ref) {
        this.occurredAt = occurredAt;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.action = action;
        this.subject = subject;
        this.ref = ref;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getAction() {
        return action;
    }

    public String getSubject() {
        return subject;
    }

    public String getRef() {
        return ref;
    }
}
