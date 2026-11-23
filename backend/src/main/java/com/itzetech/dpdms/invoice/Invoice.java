package com.itzetech.dpdms.invoice;

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
@Table(name = "invoice")
public class Invoice {

    /** OVERDUE is not here on purpose: it is worked out from the due date, never stored. */
    public enum Status { DRAFT, SENT, PARTIALLY_PAID, PAID, VOID }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20, unique = true)
    private String number;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private long subtotal;

    @Column(name = "tax_amount", nullable = false)
    private long taxAmount;

    @Column(nullable = false)
    private long total;

    @Column(name = "paid_amount", nullable = false)
    private long paidAmount;

    @Column(length = 1000)
    private String note;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", length = 300)
    private String voidReason;

    protected Invoice() {
    }

    public Invoice(String number, UUID projectId, UUID companyId, UUID createdBy) {
        this.number = number;
        this.projectId = projectId;
        this.companyId = companyId;
        this.createdBy = createdBy;
    }

    /** Replaces everything a person can edit on a draft. */
    public void setContent(UUID projectId, LocalDate issueDate, LocalDate dueDate, BigDecimal taxRate, String note,
                           long subtotal, long taxAmount) {
        this.projectId = projectId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.taxRate = taxRate;
        this.note = note;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.total = subtotal + taxAmount;
        this.updatedAt = Instant.now();
    }

    public boolean isOverdue(LocalDate today) {
        return (status == Status.SENT || status == Status.PARTIALLY_PAID) && dueDate.isBefore(today);
    }

    /** What the client still owes; nothing for a void invoice. */
    public long balance() {
        return status == Status.VOID ? 0 : total - paidAmount;
    }

    /** Visible to the client company: it has been sent. */
    public boolean wasSent() {
        return sentAt != null;
    }

    public UUID getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public long getTaxAmount() {
        return taxAmount;
    }

    public long getTotal() {
        return total;
    }

    public long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(long paidAmount) {
        this.paidAmount = paidAmount;
        this.updatedAt = Instant.now();
    }

    public String getNote() {
        return note;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void markSent() {
        this.sentAt = Instant.now();
        this.status = Status.SENT;
        this.updatedAt = this.sentAt;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void markVoid(String reason) {
        this.status = Status.VOID;
        this.voidedAt = Instant.now();
        this.voidReason = reason;
        this.updatedAt = this.voidedAt;
    }
}
