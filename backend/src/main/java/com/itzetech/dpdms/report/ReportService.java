package com.itzetech.dpdms.report;

import com.itzetech.dpdms.common.Csv;
import com.itzetech.dpdms.common.Params;
import com.itzetech.dpdms.config.AppProperties;
import com.itzetech.dpdms.invoice.Invoice;
import com.itzetech.dpdms.invoice.InvoicePayment;
import com.itzetech.dpdms.invoice.InvoicePaymentRepository;
import com.itzetech.dpdms.invoice.InvoiceRepository;
import com.itzetech.dpdms.project.Names;
import com.itzetech.dpdms.project.Project;
import com.itzetech.dpdms.project.ProjectRepository;
import com.itzetech.dpdms.project.ProjectService;
import com.itzetech.dpdms.project.ProjectTask;
import com.itzetech.dpdms.project.ProjectTaskRepository;
import com.itzetech.dpdms.project.TimeLog;
import com.itzetech.dpdms.project.TimeLogRepository;
import com.itzetech.dpdms.renewal.Renewal;
import com.itzetech.dpdms.renewal.RenewalRepository;
import com.itzetech.dpdms.report.ReportDtos.ClientRevenue;
import com.itzetech.dpdms.report.ReportDtos.DeveloperHours;
import com.itzetech.dpdms.report.ReportDtos.Hours;
import com.itzetech.dpdms.report.ReportDtos.MonthRow;
import com.itzetech.dpdms.report.ReportDtos.PriorityRow;
import com.itzetech.dpdms.report.ReportDtos.ProfitRow;
import com.itzetech.dpdms.report.ReportDtos.Projects;
import com.itzetech.dpdms.report.ReportDtos.Renewals;
import com.itzetech.dpdms.report.ReportDtos.Revenue;
import com.itzetech.dpdms.report.ReportDtos.Summary;
import com.itzetech.dpdms.report.ReportDtos.Tickets;
import com.itzetech.dpdms.report.ReportDtos.TypeRow;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.ticket.Ticket;
import com.itzetech.dpdms.ticket.TicketRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Management reports: revenue, delivery, support, renewals and hours. */
@Service
public class ReportService {

    private static final Set<Invoice.Status> BILLED = EnumSet.of(Invoice.Status.SENT, Invoice.Status.PARTIALLY_PAID, Invoice.Status.PAID);

    private final InvoiceRepository invoices;
    private final InvoicePaymentRepository payments;
    private final ProjectRepository projects;
    private final ProjectTaskRepository tasks;
    private final TimeLogRepository timeLogs;
    private final TicketRepository tickets;
    private final RenewalRepository renewals;
    private final Names names;
    private final AppProperties props;

    public ReportService(InvoiceRepository invoices, InvoicePaymentRepository payments, ProjectRepository projects,
                         ProjectTaskRepository tasks, TimeLogRepository timeLogs, TicketRepository tickets,
                         RenewalRepository renewals, Names names, AppProperties props) {
        this.invoices = invoices;
        this.payments = payments;
        this.projects = projects;
        this.tasks = tasks;
        this.timeLogs = timeLogs;
        this.tickets = tickets;
        this.renewals = renewals;
        this.names = names;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public Summary summary(AuthUser me, LocalDate from, LocalDate to) {
        ProjectService.requireManagement(me);
        Params.checkRange(from, to);
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        long rate = props.hourlyRate();
        Map<UUID, List<ProjectTask>> tasksByProject = tasks.findAll().stream().collect(Collectors.groupingBy(ProjectTask::getProjectId));
        List<Project> allProjects = projects.findAllByOrderByCreatedAtDesc();

        // revenue: invoices by issue date
        List<Invoice> billed = invoices.findAll().stream().filter(i -> BILLED.contains(i.getStatus()))
                .filter(i -> from == null || !i.getIssueDate().isBefore(from)).filter(i -> to == null || !i.getIssueDate().isAfter(to)).toList();
        long invoiced = billed.stream().mapToLong(Invoice::getTotal).sum();
        long paid = billed.stream().mapToLong(Invoice::getPaidAmount).sum();
        List<Invoice> overdue = billed.stream().filter(i -> i.isOverdue(today)).toList();
        Map<UUID, String> companyNames = names.companies(billed.stream().map(Invoice::getCompanyId).toList());
        List<ClientRevenue> byClient = billed.stream().collect(Collectors.groupingBy(Invoice::getCompanyId)).entrySet().stream()
                .map(e -> new ClientRevenue(e.getKey(), companyNames.get(e.getKey()), e.getValue().stream().mapToLong(Invoice::getTotal).sum(),
                        e.getValue().stream().mapToLong(Invoice::getPaidAmount).sum()))
                .sorted(Comparator.comparingLong(ClientRevenue::invoiced).reversed().thenComparing(ClientRevenue::companyName)).toList();
        Revenue revenue = new Revenue(invoiced, billed.size(), paid, invoiced - paid, overdue.stream().mapToLong(Invoice::balance).sum(),
                overdue.size(), byClient);

        // projects by start date
        List<Project> inRange = allProjects.stream().filter(p -> from == null || !p.getStartDate().isBefore(from))
                .filter(p -> to == null || !p.getStartDate().isAfter(to)).toList();
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (Project.Status s : Project.Status.values()) {
            byStatus.put(s.name(), (int) inRange.stream().filter(p -> p.getStatus() == s).count());
        }
        Double avg = inRange.isEmpty() ? null : Math.round(inRange.stream()
                .mapToInt(p -> percent(tasksByProject.getOrDefault(p.getId(), List.of()))).average().orElse(0) * 10) / 10.0;

        // tickets by creation date
        ZoneId zone = ZoneId.systemDefault();
        Instant start = from == null ? null : from.atStartOfDay(zone).toInstant();
        Instant end = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();
        List<Ticket> ts = tickets.findAll().stream().filter(t -> start == null || !t.getCreatedAt().isBefore(start))
                .filter(t -> end == null || t.getCreatedAt().isBefore(end)).toList();
        List<Ticket> resolved = ts.stream().filter(t -> t.getResolvedAt() != null).toList();
        Integer within = resolved.isEmpty() ? null
                : (int) Math.round(100.0 * resolved.stream().filter(t -> Boolean.TRUE.equals(t.getResolvedWithinSla())).count() / resolved.size());
        Double avgHours = resolved.isEmpty() ? null : BigDecimal.valueOf(resolved.stream()
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMillis()).average().orElse(0) / 3_600_000.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
        List<PriorityRow> byPriority = new ArrayList<>();
        for (Ticket.Priority p : Ticket.Priority.values()) {
            List<Ticket> of = ts.stream().filter(t -> t.getPriority() == p).toList();
            byPriority.add(new PriorityRow(p.name(), of.size(), (int) of.stream().filter(Ticket::isActive).count(),
                    (int) of.stream().filter(t -> t.isBreached(now)).count()));
        }
        Tickets ticketBlock = new Tickets(ts.size(), (int) ts.stream().filter(Ticket::isActive).count(),
                (int) ts.stream().filter(t -> t.isBreached(now)).count(), resolved.size(), within, avgHours, byPriority);

        // renewals: current state
        List<Renewal> rs = renewals.findAll();
        List<TypeRow> byType = new ArrayList<>();
        for (Renewal.Type type : Renewal.Type.values()) {
            List<Renewal> of = rs.stream().filter(r -> r.getType() == type).toList();
            byType.add(new TypeRow(type.name(), count(of, Renewal.Status.ACTIVE, today), count(of, Renewal.Status.EXPIRING, today),
                    count(of, Renewal.Status.EXPIRED, today)));
        }
        Renewals renewalBlock = new Renewals(rs.size(), count(rs, Renewal.Status.EXPIRING, today), count(rs, Renewal.Status.EXPIRED, today), byType);

        // hours by work date
        List<TimeLog> logs = timeLogs.findAll().stream().filter(l -> from == null || !l.getWorkDate().isBefore(from))
                .filter(l -> to == null || !l.getWorkDate().isAfter(to)).toList();
        Map<UUID, BigDecimal> perUser = new LinkedHashMap<>();
        logs.forEach(l -> perUser.merge(l.getUserId(), l.getHours(), BigDecimal::add));
        Map<UUID, String> people = names.users(perUser.keySet());
        List<DeveloperHours> byDeveloper = perUser.entrySet().stream()
                .map(e -> new DeveloperHours(e.getKey(), people.get(e.getKey()), e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(DeveloperHours::hours).reversed().thenComparing(DeveloperHours::fullName)).toList();
        Hours hours = new Hours(perUser.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP), byDeveloper);

        // profit per project
        Map<UUID, String> projectCompanies = names.companies(allProjects.stream().map(Project::getCompanyId).toList());
        List<ProfitRow> profit = new ArrayList<>();
        for (Project p : allProjects) {
            List<ProjectTask> pts = tasksByProject.getOrDefault(p.getId(), List.of());
            BigDecimal logged = pts.stream().map(ProjectTask::getLoggedHours).reduce(BigDecimal.ZERO, BigDecimal::add);
            int planned = pts.stream().mapToInt(ProjectTask::getPlannedHours).sum();
            BigDecimal forecastHours = pts.stream().map(t -> t.getLoggedHours().max(BigDecimal.valueOf(t.getPlannedHours()))).reduce(BigDecimal.ZERO, BigDecimal::add);
            long cost = logged.multiply(BigDecimal.valueOf(rate)).setScale(0, RoundingMode.HALF_UP).longValue();
            long forecast = forecastHours.multiply(BigDecimal.valueOf(rate)).setScale(0, RoundingMode.HALF_UP).longValue();
            Integer margin = p.getBudget() == 0 ? null : (int) Math.round(100.0 * (p.getBudget() - forecast) / p.getBudget());
            profit.add(new ProfitRow(p.getId(), p.getName(), projectCompanies.get(p.getCompanyId()), p.getStatus().name(), p.getBudget(),
                    logged, cost, planned, forecast, margin));
        }
        return new Summary(from, to, now, rate, revenue, new Projects(inRange.size(), byStatus, avg), ticketBlock, renewalBlock, hours, profit);
    }

    @Transactional(readOnly = true)
    public List<MonthRow> revenueByMonth(AuthUser me, int months) {
        ProjectService.requireManagement(me);
        YearMonth last = YearMonth.from(LocalDate.now());
        YearMonth first = last.minusMonths(months - 1L);
        Map<YearMonth, Long> invoiced = new LinkedHashMap<>();
        Map<YearMonth, Long> paid = new LinkedHashMap<>();
        invoices.findAll().stream().filter(i -> BILLED.contains(i.getStatus())).forEach(i -> invoiced.merge(YearMonth.from(i.getIssueDate()), i.getTotal(), Long::sum));
        for (InvoicePayment p : payments.findByReversedFalseAndPaidOnBetween(first.atDay(1), last.atEndOfMonth())) {
            paid.merge(YearMonth.from(p.getPaidOn()), p.getAmount(), Long::sum);
        }
        List<MonthRow> out = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            out.add(new MonthRow(m.toString(), invoiced.getOrDefault(m, 0L), paid.getOrDefault(m, 0L)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> summaryCsv(AuthUser me, LocalDate from, LocalDate to) {
        Summary s = summary(me, from, to);
        Csv csv = new Csv().row("Section", "Item", "Value");
        csv.row("Revenue", "Invoiced", s.revenue().invoiced()).row("Revenue", "Invoice count", s.revenue().invoiceCount())
                .row("Revenue", "Paid", s.revenue().paid()).row("Revenue", "Outstanding", s.revenue().outstanding())
                .row("Revenue", "Overdue", s.revenue().overdue()).row("Revenue", "Overdue count", s.revenue().overdueCount());
        s.revenue().byClient().forEach(c -> csv.row("Revenue by client", c.companyName() + " invoiced", c.invoiced())
                .row("Revenue by client", c.companyName() + " paid", c.paid()));
        s.projects().byStatus().forEach((k, v) -> csv.row("Projects", k, v));
        csv.row("Projects", "Average progress", s.projects().averageProgress());
        csv.row("Tickets", "Total", s.tickets().total()).row("Tickets", "Open", s.tickets().open())
                .row("Tickets", "Breached", s.tickets().breached()).row("Tickets", "Resolved", s.tickets().resolved())
                .row("Tickets", "Resolved within SLA %", s.tickets().resolvedWithinSlaPercent())
                .row("Tickets", "Average resolution hours", s.tickets().averageResolutionHours());
        s.tickets().byPriority().forEach(p -> csv.row("Tickets", p.priority() + " total", p.total()).row("Tickets", p.priority() + " open", p.open())
                .row("Tickets", p.priority() + " breached", p.breached()));
        csv.row("Renewals", "Total", s.renewals().total()).row("Renewals", "Expiring", s.renewals().expiring()).row("Renewals", "Expired", s.renewals().expired());
        s.renewals().byType().forEach(t -> csv.row("Renewals", t.type() + " active", t.active()).row("Renewals", t.type() + " expiring", t.expiring())
                .row("Renewals", t.type() + " expired", t.expired()));
        csv.row("Hours", "Total", s.hours().total());
        s.hours().byDeveloper().forEach(d -> csv.row("Hours", d.fullName(), d.hours()));
        s.projectProfit().forEach(p -> csv.row("Project profit", p.projectName() + " budget", p.budget())
                .row("Project profit", p.projectName() + " cost so far", p.costSoFar())
                .row("Project profit", p.projectName() + " forecast cost", p.forecastCost())
                .row("Project profit", p.projectName() + " forecast margin %", p.forecastMarginPercent()));
        return csv.download("reports-summary.csv");
    }

    private static int count(List<Renewal> list, Renewal.Status status, LocalDate today) {
        return (int) list.stream().filter(r -> r.status(today) == status).count();
    }

    /** Share of planned hours that is done, as a whole percentage (same rule as the project screens). */
    private static int percent(List<ProjectTask> ts) {
        int total = ts.stream().mapToInt(ProjectTask::getPlannedHours).sum();
        if (total == 0) {
            return 0;
        }
        int done = ts.stream().filter(t -> t.getStatus() == ProjectTask.Status.DONE).mapToInt(ProjectTask::getPlannedHours).sum();
        return (int) Math.round(100.0 * done / total);
    }
}
