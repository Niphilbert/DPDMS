package com.itzetech.dpdms.renewal;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request and response shapes of the renewal endpoints. See docs/api-stage3.md. */
public final class RenewalDtos {

    private RenewalDtos() {
    }

    public record CreateInput(
            @NotNull(message = "Choose the project.") UUID projectId,
            @NotNull(message = "Choose what expires.") Renewal.Type type,
            @NotBlank(message = "Enter the name, for example the domain.") @Size(max = 200, message = "Keep the name under 200 characters.") String label,
            @Size(max = 150, message = "Keep the provider under 150 characters.") String provider,
            @NotNull(message = "Choose the expiry date.") LocalDate expiresOn,
            @NotNull(message = "Enter the price.") @Min(value = 0, message = "The price cannot be negative.") Long price,
            @Size(max = 500, message = "Keep the note under 500 characters.") String note) {
    }

    public record UpdateInput(
            @NotNull(message = "Choose what expires.") Renewal.Type type,
            @NotBlank(message = "Enter the name, for example the domain.") @Size(max = 200, message = "Keep the name under 200 characters.") String label,
            @Size(max = 150, message = "Keep the provider under 150 characters.") String provider,
            @NotNull(message = "Choose the expiry date.") LocalDate expiresOn,
            @NotNull(message = "Enter the price.") @Min(value = 0, message = "The price cannot be negative.") Long price,
            @Size(max = 500, message = "Keep the note under 500 characters.") String note) {
    }

    public record RenewInput(
            @NotNull(message = "Choose the new expiry date.") LocalDate newExpiresOn,
            @Min(value = 0, message = "The price cannot be negative.") Long price,
            @Size(max = 500, message = "Keep the note under 500 characters.") String note) {
    }

    public record HistoryDto(UUID id, LocalDate renewedOn, LocalDate previousExpiresOn, LocalDate newExpiresOn, long price,
                             String note, String renewedByName) {
    }

    /** {@code price} is left out for developers, {@code note} for clients, {@code history} on lists. */
    public record RenewalDto(UUID id, UUID projectId, String projectName, UUID companyId, String companyName,
                             Renewal.Type type, String label, String provider, LocalDate expiresOn, long daysLeft,
                             Renewal.Status status,
                             @JsonInclude(JsonInclude.Include.NON_NULL) Long price,
                             @JsonInclude(JsonInclude.Include.NON_NULL) String note,
                             LocalDate lastRenewedOn, int renewalCount,
                             @JsonInclude(JsonInclude.Include.NON_NULL) List<HistoryDto> history,
                             Instant createdAt, Instant updatedAt) {
    }

    public record RunRequest(LocalDate today) {
    }

    public record ReminderItem(UUID renewalId, Renewal.Type type, String label, String projectName, LocalDate expiresOn,
                               long daysLeft, int threshold) {
    }

    public record ReminderRun(LocalDate today, int checked, int sent, List<ReminderItem> reminders) {
    }
}
