package com.itzetech.dpdms.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "one_time_code")
class OneTimeCode {

    enum Purpose {EMAIL_VERIFICATION, ADMIN_SIGN_IN}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected OneTimeCode() {
    }

    OneTimeCode(UUID userId, Purpose purpose, String codeHash, Instant expiresAt) {
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getCodeHash() {
        return codeHash;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    int getAttempts() {
        return attempts;
    }

    void addAttempt() {
        attempts++;
    }

    boolean isUsed() {
        return used;
    }

    void markUsed() {
        used = true;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
