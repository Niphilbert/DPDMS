package com.itzetech.dpdms.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** A support request from a client, with an answer deadline (SLA) that depends on its priority. */
@Entity
@Table(name = "ticket")
public class Ticket {

    /** Answer deadline in calendar hours. */
    public enum Priority {
        URGENT(4), HIGH(8), MEDIUM(24), LOW(72);

        private final int slaHours;

        Priority(int slaHours) {
            this.slaHours = slaHours;
        }

        public int slaHours() {
            return slaHours;
        }
    }

    public enum Status { OPEN, IN_PROGRESS, WAITING_CLIENT, RESOLVED, CLOSED }

    /** Derived, for the screens: where the ticket stands against its deadline. */
    public enum SlaState { ON_TRACK, AT_RISK, BREACHED, MET, MISSED }

    /** Statuses of a ticket that is still being worked on: the SLA clock runs. */
    public static final Set<Status> ACTIVE = EnumSet.of(Status.OPEN, Status.IN_PROGRESS, Status.WAITING_CLIENT);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 12, unique = true)
    private String number;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "sla_hours", nullable = false)
    private int slaHours;

    @Column(name = "sla_started_at", nullable = false)
    private Instant slaStartedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_within_sla")
    private Boolean resolvedWithinSla;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "reopen_count", nullable = false)
    private int reopenCount;

    @Column(name = "sla_warned_at")
    private Instant slaWarnedAt;

    @Column(name = "sla_breach_notified_at")
    private Instant slaBreachNotifiedAt;

    protected Ticket() {
    }

    public Ticket(String number, UUID companyId, UUID projectId, String subject, String description, Priority priority,
                  UUID createdBy, Instant now) {
        this.number = number;
        this.companyId = companyId;
        this.projectId = projectId;
        this.subject = subject;
        this.description = description;
        this.priority = priority;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
        startClock(now);
    }

    /** (Re)starts the SLA clock: the deadline is {@code from} plus the hours of the current priority. */
    public void startClock(Instant from) {
        this.slaHours = priority.slaHours();
        this.slaStartedAt = from;
        this.dueAt = from.plus(Duration.ofHours(slaHours));
        this.slaWarnedAt = null;
        this.slaBreachNotifiedAt = null;
    }

    public void changePriority(Priority p, Instant now) {
        this.priority = p;
        this.slaHours = p.slaHours();
        this.dueAt = slaStartedAt.plus(Duration.ofHours(slaHours));
        this.updatedAt = now;
    }

    public boolean isActive() {
        return ACTIVE.contains(status);
    }

    public boolean isBreached(Instant now) {
        return isActive() && now.isAfter(dueAt);
    }

    /** Whole minutes to the deadline (negative once passed); null when the ticket is no longer being worked on. */
    public Long minutesLeft(Instant now) {
        if (!isActive()) {
            return null;
        }
        return Math.floorDiv(dueAt.toEpochMilli() - now.toEpochMilli(), 60_000L);
    }

    public SlaState slaState(Instant now) {
        if (isActive()) {
            if (now.isAfter(dueAt)) {
                return SlaState.BREACHED;
            }
            long window = dueAt.toEpochMilli() - slaStartedAt.toEpochMilli();
            long left = dueAt.toEpochMilli() - now.toEpochMilli();
            return left * 4 <= window ? SlaState.AT_RISK : SlaState.ON_TRACK;
        }
        if (resolvedAt == null) {
            return null;
        }
        return Boolean.TRUE.equals(resolvedWithinSla) ? SlaState.MET : SlaState.MISSED;
    }

    public void moveTo(Status s, Instant now) {
        this.status = s;
        this.updatedAt = now;
    }

    public void resolve(Instant now) {
        this.resolvedAt = now;
        this.resolvedWithinSla = !now.isAfter(dueAt);
    }

    /** Back to work after a resolution: the resolution is forgotten and the clock starts again. */
    public void reopen(Instant now, boolean byClient) {
        this.resolvedAt = null;
        this.resolvedWithinSla = null;
        if (byClient) {
            this.reopenCount++;
        }
        startClock(now);
        moveTo(Status.IN_PROGRESS, now);
    }

    public void close(Instant now) {
        this.closedAt = now;
        moveTo(Status.CLOSED, now);
    }

    public void assignTo(UUID userId, Instant now) {
        this.assigneeId = userId;
        this.updatedAt = now;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public Status getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public Instant getSlaStartedAt() {
        return slaStartedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Boolean getResolvedWithinSla() {
        return resolvedWithinSla;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public int getReopenCount() {
        return reopenCount;
    }

    public Instant getSlaWarnedAt() {
        return slaWarnedAt;
    }

    public Instant getSlaBreachNotifiedAt() {
        return slaBreachNotifiedAt;
    }
}
