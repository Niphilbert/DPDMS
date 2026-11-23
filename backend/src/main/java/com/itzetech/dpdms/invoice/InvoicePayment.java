package com.itzetech.dpdms.invoice;

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

/** A payment recorded by hand. A wrong one is reversed, never deleted, so the history stays complete. */
@Entity
@Table(name = "invoice_payment")
public class InvoicePayment {

    public enum Method { MOBILE_MONEY, BANK_TRANSFER, CASH }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Method method;

    @Column(length = 100)
    private String reference;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    @Column(nullable = false)
    private boolean reversed;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversed_by")
    private UUID reversedBy;

    @Column(name = "reverse_reason", length = 300)
    private String reverseReason;

    protected InvoicePayment() {
    }

    public InvoicePayment(UUID invoiceId, long amount, LocalDate paidOn, Method method, String reference, UUID recordedBy) {
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.paidOn = paidOn;
        this.method = method;
        this.reference = reference;
        this.recordedBy = recordedBy;
    }

    public void reverse(UUID by, String reason) {
        this.reversed = true;
        this.reversedAt = Instant.now();
        this.reversedBy = by;
        this.reverseReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public long getAmount() {
        return amount;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public Method getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public boolean isReversed() {
        return reversed;
    }

    public Instant getReversedAt() {
        return reversedAt;
    }

    public UUID getReversedBy() {
        return reversedBy;
    }

    public String getReverseReason() {
        return reverseReason;
    }
}
