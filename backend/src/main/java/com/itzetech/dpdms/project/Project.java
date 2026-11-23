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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project")
public class Project {

    public enum Status { PLANNING, IN_PROGRESS, IN_REVIEW, LAUNCHED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PLANNING;

    @Column(name = "manager_id", nullable = false)
    private UUID managerId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Quote total plus accepted change requests, in the quote currency (whole units). */
    @Column(nullable = false)
    private long budget;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Project() {
    }

    public Project(UUID requestId, UUID companyId, String name, UUID managerId, LocalDate startDate, LocalDate dueDate, long budget) {
        this.requestId = requestId;
        this.companyId = companyId;
        this.name = name;
        this.managerId = managerId;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.budget = budget;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UUID getManagerId() {
        return managerId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getBudget() {
        return budget;
    }

    public void addToBudget(long amount) {
        this.budget += amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
