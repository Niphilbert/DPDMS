package com.itzetech.dpdms.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "time_log")
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal hours;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected TimeLog() {
    }

    public TimeLog(UUID taskId, UUID userId, LocalDate workDate, BigDecimal hours, String note) {
        this.taskId = taskId;
        this.userId = userId;
        this.workDate = workDate;
        this.hours = hours;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
