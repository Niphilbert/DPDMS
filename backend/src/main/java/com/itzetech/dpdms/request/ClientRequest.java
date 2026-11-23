package com.itzetech.dpdms.request;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A client's request for work, and the quote the project manager writes for it. */
@Entity
@Table(name = "client_request")
public class ClientRequest {

    public enum Status { NEW, QUOTED, CHANGES_REQUESTED, ACCEPTED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "budget_text", length = 100)
    private String budgetText;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Status status = Status.NEW;

    @Column(name = "change_note", length = 1000)
    private String changeNote;

    @Column(name = "quote_weeks")
    private Integer quoteWeeks;

    @Column(name = "quote_valid_until")
    private LocalDate quoteValidUntil;

    @Column(name = "quote_sent_at")
    private Instant quoteSentAt;

    @Column(name = "quoted_by")
    private UUID quotedBy;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<QuoteItem> items = new ArrayList<>();

    protected ClientRequest() {
    }

    public ClientRequest(UUID companyId, UUID createdBy, String title, String description, String budgetText, LocalDate deadline) {
        this.companyId = companyId;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.budgetText = budgetText;
        this.deadline = deadline;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBudgetText() {
        return budgetText;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public Status getStatus() {
        return status;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public Integer getQuoteWeeks() {
        return quoteWeeks;
    }

    public LocalDate getQuoteValidUntil() {
        return quoteValidUntil;
    }

    public Instant getQuoteSentAt() {
        return quoteSentAt;
    }

    public UUID getQuotedBy() {
        return quotedBy;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<QuoteItem> getItems() {
        return items;
    }

    public long total() {
        return items.stream().mapToLong(QuoteItem::getAmount).sum();
    }

    /** Replaces the quote with a new (or revised) one and hands it to the client. */
    public void quote(UUID by, List<QuoteItem> newItems, int weeks, LocalDate validUntil) {
        items.clear();
        newItems.forEach(i -> {
            i.attach(this);
            items.add(i);
        });
        this.quoteWeeks = weeks;
        this.quoteValidUntil = validUntil;
        this.quoteSentAt = Instant.now();
        this.quotedBy = by;
        this.changeNote = null;
        setStatus(Status.QUOTED);
    }

    public void requestChanges(String note) {
        this.changeNote = note;
        setStatus(Status.CHANGES_REQUESTED);
    }

    public void accept(UUID projectId) {
        this.projectId = projectId;
        setStatus(Status.ACCEPTED);
    }

    public void decline(String reason) {
        if (reason != null) {
            this.changeNote = reason;
        }
        setStatus(Status.DECLINED);
    }

    private void setStatus(Status s) {
        this.status = s;
        this.updatedAt = Instant.now();
    }
}
