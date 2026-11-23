package com.itzetech.dpdms.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String message;

    /** Where in the app the notification leads, for example "/projects/123". */
    @Column(length = 200)
    private String link;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    Notification(UUID userId, String message, String link) {
        this.userId = userId;
        this.message = message.length() > 500 ? message.substring(0, 497) + "..." : message;
        this.link = link;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}
