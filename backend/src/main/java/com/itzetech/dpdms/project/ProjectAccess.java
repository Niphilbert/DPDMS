package com.itzetech.dpdms.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Access the administrator gave a developer to a project they have no task on. */
@Entity
@Table(name = "project_access")
public class ProjectAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    protected ProjectAccess() {
    }

    public ProjectAccess(UUID projectId, UUID userId, UUID grantedBy) {
        this.projectId = projectId;
        this.userId = userId;
        this.grantedBy = grantedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }
}
