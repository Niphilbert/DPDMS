package com.itzetech.dpdms.renewal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One time a renewal was renewed. */
@Entity
@Table(name = "renewal_history")
public class RenewalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "renewal_id", nullable = false)
    private UUID renewalId;

    @Column(name = "renewed_on", nullable = false)
    private LocalDate renewedOn;

    @Column(name = "previous_expires_on", nullable = false)
    private LocalDate previousExpiresOn;

    @Column(name = "new_expires_on", nullable = false)
    private LocalDate newExpiresOn;

    @Column(nullable = false)
    private long price;

    @Column(length = 500)
    private String note;

    @Column(name = "renewed_by", nullable = false)
    private UUID renewedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected RenewalHistory() {
    }

    public RenewalHistory(UUID renewalId, LocalDate renewedOn, LocalDate previousExpiresOn, LocalDate newExpiresOn, long price,
                          String note, UUID renewedBy) {
        this.renewalId = renewalId;
        this.renewedOn = renewedOn;
        this.previousExpiresOn = previousExpiresOn;
        this.newExpiresOn = newExpiresOn;
        this.price = price;
        this.note = note;
        this.renewedBy = renewedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRenewalId() {
        return renewalId;
    }

    public LocalDate getRenewedOn() {
        return renewedOn;
    }

    public LocalDate getPreviousExpiresOn() {
        return previousExpiresOn;
    }

    public LocalDate getNewExpiresOn() {
        return newExpiresOn;
    }

    public long getPrice() {
        return price;
    }

    public String getNote() {
        return note;
    }

    public UUID getRenewedBy() {
        return renewedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
