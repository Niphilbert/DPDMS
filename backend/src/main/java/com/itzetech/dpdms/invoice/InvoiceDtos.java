package com.itzetech.dpdms.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request and response shapes of the invoice endpoints. See docs/api-stage3.md. */
public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    public record LineInput(
            @NotBlank(message = "Describe this line.") @Size(max = 200, message = "Keep the description under 200 characters.") String description,
            @NotNull(message = "Enter the quantity.") @DecimalMin(value = "0.01", message = "The quantity must be more than zero.")
            @DecimalMax(value = "100000", message = "The quantity is too large.") @Digits(integer = 6, fraction = 2, message = "Use at most 2 decimals.") BigDecimal quantity,
            @NotNull(message = "Enter the unit price.") @Min(value = 1, message = "The unit price must be at least 1 RWF.")
            @Max(value = 1_000_000_000, message = "The unit price is too large.") Long unitPrice) {
    }

    public record InvoiceInput(
            UUID projectId,
            UUID companyId,
            @NotNull(message = "Choose the issue date.") LocalDate issueDate,
            @NotNull(message = "Choose the due date.") LocalDate dueDate,
            @DecimalMin(value = "0", message = "The tax rate cannot be negative.") @DecimalMax(value = "100", message = "The tax rate cannot be more than 100.")
            @Digits(integer = 3, fraction = 2, message = "Use at most 2 decimals.") BigDecimal taxRate,
            @Size(max = 1000, message = "Keep the note under 1000 characters.") String note,
            @NotNull(message = "Add at least one line.") @Size(min = 1, max = 50, message = "An invoice needs 1 to 50 lines.") @Valid List<LineInput> lines) {
    }

    public record PaymentInput(
            @NotNull(message = "Enter the amount.") @Min(value = 1, message = "The amount must be more than zero.") Long amount,
            @NotNull(message = "Choose the payment date.") LocalDate paidOn,
            @NotNull(message = "Choose how it was paid.") InvoicePayment.Method method,
            @Size(max = 100, message = "Keep the reference under 100 characters.") String reference) {
    }

    public record ReverseInput(
            @NotBlank(message = "Give the reason.") @Size(max = 300, message = "Keep the reason under 300 characters.") String reason) {
    }

    public record VoidInput(@Size(max = 300, message = "Keep the reason under 300 characters.") String reason) {
    }

    public record LineDto(UUID id, String description, BigDecimal quantity, long unitPrice, long lineTotal) {
    }

    public record PaymentDto(UUID id, long amount, LocalDate paidOn, InvoicePayment.Method method, String reference,
                             String recordedByName, Instant recordedAt, boolean reversed, Instant reversedAt,
                             String reversedByName, String reverseReason) {
    }

    /** {@code lines} and {@code payments} are left out of list rows (they are only on the detail call). */
    public record InvoiceDto(UUID id, String number, UUID projectId, String projectName, UUID companyId, String companyName,
                             Invoice.Status status, String displayStatus, boolean overdue, Long daysOverdue,
                             LocalDate issueDate, LocalDate dueDate, BigDecimal taxRate, long subtotal, long taxAmount, long total,
                             long paidAmount, long balance, String note, String createdByName, Instant createdAt,
                             Instant sentAt, Instant voidedAt, String voidReason,
                             @JsonInclude(JsonInclude.Include.NON_NULL) List<LineDto> lines,
                             @JsonInclude(JsonInclude.Include.NON_NULL) List<PaymentDto> payments) {
    }

    public record InvoiceTotals(int invoiceCount, long invoiced, long paid, long balanceDue, int openCount,
                                int overdueCount, long overdueAmount) {
    }

    public record ProjectMoney(UUID projectId, long budget, long invoiced, long paid, long balanceDue, long unbilled,
                               long overdueAmount) {
    }

    public record SuggestedLine(String description, BigDecimal quantity, long unitPrice, long lineTotal) {
    }

    public record InvoiceSuggestion(UUID projectId, String projectName, UUID companyId, long hourlyRate, BigDecimal loggedHours,
                                    long budget, long invoiced, long unbilled, SuggestedLine timeLine, SuggestedLine balanceLine) {
    }
}
