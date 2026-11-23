package com.itzetech.dpdms.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One plain-language line in the feed every person on the project (the client too) can read. */
@Entity
@Table(name = "project_update")
public class ProjectUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ProjectUpdate() {
    }

    public ProjectUpdate(UUID projectId, String message) {
        this.projectId = projectId;
        this.message = message.length() > 300 ? message.substring(0, 297) + "..." : message;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
