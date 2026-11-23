package com.itzetech.dpdms.report;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Response shapes of the reports and the dashboard. See docs/api-stage3.md. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record ClientRevenue(UUID companyId, String companyName, long invoiced, long paid) {
    }

    public record Revenue(long invoiced, int invoiceCount, long paid, long outstanding, long overdue, int overdueCount,
                          List<ClientRevenue> byClient) {
    }

    public record Projects(int total, Map<String, Integer> byStatus, Double averageProgress) {
    }

    public record PriorityRow(String priority, int total, int open, int breached) {
    }

    public record Tickets(int total, int open, int breached, int resolved, Integer resolvedWithinSlaPercent,
                          Double averageResolutionHours, List<PriorityRow> byPriority) {
    }

    public record TypeRow(String type, int active, int expiring, int expired) {
    }

    public record Renewals(int total, int expiring, int expired, List<TypeRow> byType) {
    }

    public record DeveloperHours(UUID userId, String fullName, BigDecimal hours) {
    }

    public record Hours(BigDecimal total, List<DeveloperHours> byDeveloper) {
    }

    public record ProfitRow(UUID projectId, String projectName, String companyName, String status, long budget,
                            BigDecimal loggedHours, long costSoFar, int plannedHours, long forecastCost, Integer forecastMarginPercent) {
    }

    public record Summary(LocalDate from, LocalDate to, Instant generatedAt, long hourlyRate, Revenue revenue, Projects projects,
                          Tickets tickets, Renewals renewals, Hours hours, List<ProfitRow> projectProfit) {
    }

    public record MonthRow(String month, long invoiced, long paid) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InvoiceCounts(int unpaidCount, long unpaidAmount, int overdueCount, long overdueAmount, Integer draftCount) {
    }

    public record RenewalCounts(int expiringCount, int expiredCount) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TicketCounts(int openCount, Integer breachedCount, Integer atRiskCount, Integer unassignedCount,
                               Integer waitingOnClientCount) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Dashboard(InvoiceCounts invoices, RenewalCounts renewals, TicketCounts tickets) {
    }
}
