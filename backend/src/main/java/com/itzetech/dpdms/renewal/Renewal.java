package com.itzetech.dpdms.renewal;

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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Something that expires for a client's website: a domain, hosting plan, SSL certificate or maintenance plan. */
@Entity
@Table(name = "renewal")
public class Renewal {

    public enum Type {
        DOMAIN("Domain"), HOSTING("Hosting"), SSL("SSL certificate"), MAINTENANCE("Maintenance plan");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Never stored: worked out from the expiry date and today. */
    public enum Status { ACTIVE, EXPIRING, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Type type;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(length = 150)
    private String provider;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Column(nullable = false)
    private long price;

    @Column(length = 500)
    private String note;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Renewal() {
    }

    public Renewal(UUID projectId, UUID companyId, UUID createdBy) {
        this.projectId = projectId;
        this.companyId = companyId;
        this.createdBy = createdBy;
    }

    public void set(Type type, String label, String provider, LocalDate expiresOn, long price, String note) {
        this.type = type;
        this.label = label;
        this.provider = provider;
        this.expiresOn = expiresOn;
        this.price = price;
        this.note = note;
        this.updatedAt = Instant.now();
    }

    public void renewTo(LocalDate newExpiry, Long newPrice) {
        this.expiresOn = newExpiry;
        if (newPrice != null) {
            this.price = newPrice;
        }
        this.updatedAt = Instant.now();
    }

    public long daysLeft(LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiresOn);
    }

    public Status status(LocalDate today) {
        return statusFor(daysLeft(today));
    }

    public static Status statusFor(long daysLeft) {
        return daysLeft < 0 ? Status.EXPIRED : daysLeft <= 30 ? Status.EXPIRING : Status.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getProvider() {
        return provider;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public long getPrice() {
        return price;
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
}
