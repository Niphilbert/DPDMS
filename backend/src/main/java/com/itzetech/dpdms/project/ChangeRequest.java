package com.itzetech.dpdms.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Extra work the client asked for (or the team proposed) after the quote was accepted, with its own cost. */
@Entity
@Table(name = "change_request")
public class ChangeRequest {

    public enum Status { PROPOSED, ACCEPTED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private long cost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.PROPOSED;

    @Column(name = "proposed_by", nullable = false)
    private UUID proposedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "answered_by")
    private UUID answeredBy;

    @Column(name = "answered_at")
    private Instant answeredAt;

    protected ChangeRequest() {
    }

    public ChangeRequest(UUID projectId, String title, long cost, UUID proposedBy) {
        this.projectId = projectId;
        this.title = title;
        this.cost = cost;
        this.proposedBy = proposedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public long getCost() {
        return cost;
    }

    public Status getStatus() {
        return status;
    }

    public UUID getProposedBy() {
        return proposedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getAnsweredBy() {
        return answeredBy;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void answer(Status status, UUID by) {
        this.status = status;
        this.answeredBy = by;
        this.answeredAt = Instant.now();
    }
}
