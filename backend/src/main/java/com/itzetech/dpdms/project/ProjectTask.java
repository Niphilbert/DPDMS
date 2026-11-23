package com.itzetech.dpdms.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project_task")
public class ProjectTask {

    public enum Status { TODO, DOING, REVIEW, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    /** 0 Discovery, 1 Design, 2 Build, 3 Test and launch. */
    @Column(nullable = false)
    private int phase;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.TODO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "planned_hours", nullable = false)
    private int plannedHours;

    @Column(name = "logged_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal loggedHours = BigDecimal.ZERO;

    @Column(name = "submission_file", length = 300)
    private String submissionFile;

    @Column(name = "submission_note", length = 1000)
    private String submissionNote;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "sent_back_note", length = 1000)
    private String sentBackNote;

    @Column(name = "sent_back_at")
    private Instant sentBackAt;

    @Column(name = "sent_back_by")
    private UUID sentBackBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ProjectTask() {
    }

    public ProjectTask(UUID projectId, int phase, int sortOrder, String title, UUID assigneeId, LocalDate dueDate, int plannedHours) {
        this.projectId = projectId;
        this.phase = phase;
        this.sortOrder = sortOrder;
        this.title = title;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
        this.plannedHours = plannedHours;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public int getPhase() {
        return phase;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getTitle() {
        return title;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getPlannedHours() {
        return plannedHours;
    }

    public BigDecimal getLoggedHours() {
        return loggedHours;
    }

    public void addLoggedHours(BigDecimal hours) {
        this.loggedHours = this.loggedHours.add(hours);
    }

    public String getSubmissionFile() {
        return submissionFile;
    }

    public String getSubmissionNote() {
        return submissionNote;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public String getSentBackNote() {
        return sentBackNote;
    }

    public Instant getSentBackAt() {
        return sentBackAt;
    }

    public UUID getSentBackBy() {
        return sentBackBy;
    }

    public void submit(UUID by, String file, String note) {
        this.submissionFile = file;
        this.submissionNote = note;
        this.submittedAt = Instant.now();
        this.submittedBy = by;
        this.sentBackNote = null;
        this.sentBackAt = null;
        this.sentBackBy = null;
        this.status = Status.REVIEW;
    }

    public void sendBack(UUID by, String note) {
        this.sentBackNote = note;
        this.sentBackAt = Instant.now();
        this.sentBackBy = by;
        this.status = Status.DOING;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isLate(LocalDate today) {
        return status != Status.DONE && dueDate.isBefore(today);
    }
}
