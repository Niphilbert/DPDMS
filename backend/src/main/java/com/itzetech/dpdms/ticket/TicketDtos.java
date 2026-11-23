package com.itzetech.dpdms.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request and response shapes of the ticket endpoints. See docs/api-stage3.md. */
public final class TicketDtos {

    private TicketDtos() {
    }

    public record CreateInput(
            @NotBlank(message = "Add a short title.") @Size(max = 200, message = "Keep the title under 200 characters.") String subject,
            @NotBlank(message = "Describe the problem.") @Size(max = 4000, message = "Keep the description under 4000 characters.") String description,
            @NotNull(message = "Choose how urgent it is.") Ticket.Priority priority,
            UUID projectId) {
    }

    public record CommentInput(
            @NotBlank(message = "Write a message first.") @Size(max = 4000, message = "Keep the message under 4000 characters.") String body,
            Boolean internal) {
    }

    public record StatusInput(@NotNull(message = "Choose a status.") Ticket.Status status) {
    }

    public record AssignInput(@NotNull(message = "Choose who will handle this ticket.") UUID assigneeId) {
    }

    public record PriorityInput(@NotNull(message = "Choose a priority.") Ticket.Priority priority) {
    }

    public record CommentDto(UUID id, UUID authorId, String authorName, String authorRole, String body, boolean internal, Instant createdAt) {
    }

    /** {@code description} and {@code comments} are only on the detail call and left out of list rows. */
    public record TicketDto(UUID id, String number, String subject, Ticket.Priority priority, Ticket.Status status,
                            UUID companyId, String companyName, UUID projectId, String projectName,
                            UUID createdById, String createdByName, UUID assigneeId, String assigneeName,
                            int slaHours, Instant createdAt, Instant dueAt, boolean breached, Long minutesLeft,
                            Ticket.SlaState slaState, Instant resolvedAt, Boolean resolvedWithinSla, Instant closedAt,
                            int reopenCount, int commentCount, Instant updatedAt,
                            @JsonInclude(JsonInclude.Include.NON_NULL) String description,
                            @JsonInclude(JsonInclude.Include.NON_NULL) List<CommentDto> comments) {
    }

    public record SlaRequest(Instant now, LocalDate today) {
    }

    public record SlaAlert(UUID ticketId, String number, String kind, String assigneeName) {
    }

    public record SlaRun(Instant now, int checked, int atRisk, int breached, List<SlaAlert> alerts) {
    }
}
