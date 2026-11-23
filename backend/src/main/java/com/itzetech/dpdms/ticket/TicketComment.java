package com.itzetech.dpdms.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One message on a ticket. An internal one is a note between staff and is never shown to the client. */
@Entity
@Table(name = "ticket_comment")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean internal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TicketComment() {
    }

    public TicketComment(UUID ticketId, UUID authorId, String body, boolean internal, Instant createdAt) {
        this.ticketId = ticketId;
        this.authorId = authorId;
        this.body = body;
        this.internal = internal;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public boolean isInternal() {
        return internal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
