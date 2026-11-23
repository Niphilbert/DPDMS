package com.itzetech.dpdms.invoice;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.common.Csv;
import com.itzetech.dpdms.common.Money;
import com.itzetech.dpdms.common.Params;
import com.itzetech.dpdms.config.AppProperties;
import com.itzetech.dpdms.invoice.InvoiceDtos.InvoiceDto;
import com.itzetech.dpdms.invoice.InvoiceDtos.InvoiceInput;
import com.itzetech.dpdms.invoice.InvoiceDtos.InvoiceSuggestion;
import com.itzetech.dpdms.invoice.InvoiceDtos.InvoiceTotals;
import com.itzetech.dpdms.invoice.InvoiceDtos.LineDto;
import com.itzetech.dpdms.invoice.InvoiceDtos.LineInput;
import com.itzetech.dpdms.invoice.InvoiceDtos.PaymentDto;
import com.itzetech.dpdms.invoice.InvoiceDtos.PaymentInput;
import com.itzetech.dpdms.invoice.InvoiceDtos.ProjectMoney;
import com.itzetech.dpdms.invoice.InvoiceDtos.SuggestedLine;
import com.itzetech.dpdms.notification.Messenger;
import com.itzetech.dpdms.project.Names;
import com.itzetech.dpdms.project.Project;
import com.itzetech.dpdms.project.ProjectRepository;
import com.itzetech.dpdms.project.ProjectService;
import com.itzetech.dpdms.project.ProjectTask;
import com.itzetech.dpdms.project.ProjectTaskRepository;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.user.ClientCompany;
import com.itzetech.dpdms.user.ClientCompanyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Invoices and payments. Developers never get here: money is for management and the client company. */
@Service
public class InvoiceService {

    /** Statuses that count as money owed to us: drafts and void invoices never do. */
    static final Set<Invoice.Status> BILLED = EnumSet.of(Invoice.Status.SENT, Invoice.Status.PARTIALLY_PAID, Invoice.Status.PAID);

    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;
    private final InvoicePaymentRepository payments;
    private final ProjectService projectService;
    private final ProjectRepository projects;
    private final ProjectTaskRepository tasks;
    private final ClientCompanyRepository companies;
    private final Names names;
    private final Messenger messenger;
    private final AuditService audit;
    private final JdbcTemplate jdbc;
    private final AppProperties props;

    public InvoiceService(InvoiceRepository invoices, InvoiceLineRepository lines, InvoicePaymentRepository payments,
                          ProjectService projectService, ProjectRepository projects, ProjectTaskRepository tasks, ClientCompanyRepository companies,
                          Names names, Messenger messenger, AuditService audit, JdbcTemplate jdbc, AppProperties props) {
        this.invoices = invoices;
        this.lines = lines;
        this.payments = payments;
        this.projectService = projectService;
        this.projects = projects;
        this.tasks = tasks;
        this.companies = companies;
        this.names = names;
        this.messenger = messenger;
        this.audit = audit;
        this.jdbc = jdbc;
        this.props = props;
    }

    /** The optional filters of the list, the CSV export and the summary. */
    public record Filter(String status, UUID projectId, UUID companyId, LocalDate from, LocalDate to) {
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<InvoiceDto> list(AuthUser me, Filter f) {
        return toDtos(me, select(me, f), false);
    }

    @Transactional(readOnly = true)
    public InvoiceDto detail(AuthUser me, UUID id) {
        return toDtos(me, List.of(visible(me, id)), true).get(0);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportCsv(AuthUser me, Filter f) {
        ProjectService.requireManagement(me);
        Csv csv = new Csv().row("Number", "Status", "Client", "Project", "Issue date", "Due date", "Subtotal", "Tax", "Total", "Paid", "Balance");
        for (InvoiceDto i : toDtos(me, select(me, f), false)) {
            csv.row(i.number(), i.displayStatus(), i.companyName(), i.projectName(), i.issueDate(), i.dueDate(),
                    i.subtotal(), i.taxAmount(), i.total(), i.paidAmount(), i.balance());
        }
        return csv.download("invoices.csv");
    }

    @Transactional(readOnly = true)
    public InvoiceTotals summary(AuthUser me, Filter f) {
        List<Invoice> billed = select(me, f).stream().filter(i -> BILLED.contains(i.getStatus())).toList();
        return totals(billed, LocalDate.now());
    }

    static InvoiceTotals totals(Collection<Invoice> billed, LocalDate today) {
        long invoiced = 0, paid = 0, overdueAmount = 0;
        int open = 0, overdue = 0;
        for (Invoice i : billed) {
            invoiced += i.getTotal();
            paid += i.getPaidAmount();
            if (i.getStatus() == Invoice.Status.SENT || i.getStatus() == Invoice.Status.PARTIALLY_PAID) {
                open++;
            }
            if (i.isOverdue(today)) {
                overdue++;
                overdueAmount += i.balance();
            }
        }
        return new InvoiceTotals(billed.size(), invoiced, paid, invoiced - paid, open, overdue, overdueAmount);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> forProject(AuthUser me, UUID projectId) {
        Project p = projectService.visible(me, projectId);
        ProjectService.requireNotDeveloper(me);
        List<Invoice> found = invoices.findByProjectId(p.getId()).stream()
                .filter(i -> !me.isClient() || i.wasSent()).collect(Collectors.toList());
        return toDtos(me, sorted(found), false);
    }

    @Transactional(readOnly = true)
    public ProjectMoney projectMoney(AuthUser me, UUID projectId) {
        Project p = projectService.visible(me, projectId);
        ProjectService.requireNotDeveloper(me);
        List<Invoice> billed = invoices.findByProjectId(p.getId()).stream().filter(i -> BILLED.contains(i.getStatus())).toList();
        InvoiceTotals t = totals(billed, LocalDate.now());
        return new ProjectMoney(p.getId(), p.getBudget(), t.invoiced(), t.paid(), t.balanceDue(),
                Math.max(0, p.getBudget() - t.invoiced()), t.overdueAmount());
    }

    @Transactional(readOnly = true)
    public InvoiceSuggestion suggestion(AuthUser me, UUID projectId) {
        Project p = projectService.visible(me, projectId);
        ProjectService.requireManagement(me);
        BigDecimal logged = tasks.findByProjectIdIn(List.of(p.getId())).stream().map(ProjectTask::getLoggedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long rate = props.hourlyRate();
        long invoiced = invoices.findByProjectId(p.getId()).stream().filter(i -> BILLED.contains(i.getStatus()))
                .mapToLong(Invoice::getTotal).sum();
        long unbilled = Math.max(0, p.getBudget() - invoiced);
        SuggestedLine timeLine = logged.signum() > 0 && rate > 0
                ? new SuggestedLine("Hours logged to date (" + logged.stripTrailingZeros().toPlainString() + " h)", logged, rate, Money.lineTotal(logged, rate))
                : null;
        SuggestedLine balanceLine = unbilled > 0 ? new SuggestedLine("Remaining budget balance", BigDecimal.ONE, unbilled, unbilled) : null;
        return new InvoiceSuggestion(p.getId(), p.getName(), p.getCompanyId(), rate, logged, p.getBudget(), invoiced, unbilled, timeLine, balanceLine);
    }

    // ---------------------------------------------------------------- writing

    @Transactional
    public InvoiceDto create(AuthUser me, InvoiceInput in) {
        ProjectService.requireManagement(me);
        Resolved r = resolve(me, in, null);
        checkDates(in);
        String number = nextNumber(in.issueDate().getYear());
        Invoice inv = new Invoice(number, r.projectId, r.companyId, me.id());
        applyContent(inv, in, r.projectId);
        inv = invoices.save(inv);
        saveLines(inv, in.lines());
        audit.record(me, "Invoice created", number + " · " + names.company(inv.getCompanyId()) + " · " + Money.format(inv.getTotal()));
        return toDtos(me, List.of(inv), true).get(0);
    }

    @Transactional
    public InvoiceDto update(AuthUser me, UUID id, InvoiceInput in) {
        ProjectService.requireManagement(me);
        Invoice inv = invoices.findById(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        if (inv.getStatus() != Invoice.Status.DRAFT) {
            throw ApiException.conflict("INVOICE_NOT_DRAFT", "Only a draft invoice can be changed.");
        }
        Resolved r = resolve(me, in, inv.getCompanyId());
        checkDates(in);
        applyContent(inv, in, r.projectId);
        lines.deleteByInvoiceId(inv.getId());
        lines.flush();
        saveLines(inv, in.lines());
        audit.record(me, "Invoice updated", inv.getNumber() + " · " + Money.format(inv.getTotal()));
        return toDtos(me, List.of(inv), true).get(0);
    }

    @Transactional
    public InvoiceDto send(AuthUser me, UUID id) {
        ProjectService.requireManagement(me);
        Invoice inv = invoices.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        if (inv.getStatus() != Invoice.Status.DRAFT) {
            throw ApiException.conflict("INVOICE_NOT_DRAFT", "This invoice has already been sent.");
        }
        inv.markSent();
        messenger.toCompany(inv.getCompanyId(), "New invoice " + inv.getNumber(),
                "New invoice " + inv.getNumber() + " from Itzetech: " + Money.format(inv.getTotal()) + ", due " + inv.getDueDate() + ".",
                link(inv));
        audit.record(me, "Invoice sent", inv.getNumber() + " · " + names.company(inv.getCompanyId()) + " · " + Money.format(inv.getTotal()));
        return toDtos(me, List.of(inv), true).get(0);
    }

    @Transactional
    public InvoiceDto recordPayment(AuthUser me, UUID id, PaymentInput in) {
        ProjectService.requireManagement(me);
        Invoice inv = invoices.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        if (inv.getStatus() != Invoice.Status.SENT && inv.getStatus() != Invoice.Status.PARTIALLY_PAID) {
            throw ApiException.conflict("INVOICE_NOT_PAYABLE", inv.getStatus() == Invoice.Status.PAID
                    ? "This invoice is already paid in full." : "Payments can only be recorded on a sent invoice.");
        }
        if (in.paidOn().isAfter(LocalDate.now())) {
            throw ApiException.badRequest("DATE_IN_FUTURE", "The payment date cannot be in the future.");
        }
        if (in.amount() > inv.balance()) {
            throw ApiException.conflict("OVERPAYMENT", "The payment is more than the balance of " + Money.format(inv.balance()) + ".");
        }
        String ref = in.reference() == null || in.reference().isBlank() ? null : in.reference().trim();
        payments.save(new InvoicePayment(inv.getId(), in.amount(), in.paidOn(), in.method(), ref, me.id()));
        recompute(inv);
        messenger.toCompany(inv.getCompanyId(), null, "We received your payment of " + Money.format(in.amount()) + " for invoice " + inv.getNumber()
                + (inv.balance() == 0 ? ". It is now paid in full. Thank you." : ". The balance is " + Money.format(inv.balance()) + "."), link(inv));
        audit.record(me, "Payment recorded", inv.getNumber() + " · " + Money.format(in.amount()) + " · " + in.method());
        return toDtos(me, List.of(inv), true).get(0);
    }

    @Transactional
    public InvoiceDto reversePayment(AuthUser me, UUID id, UUID paymentId, String reason) {
        ProjectService.requireManagement(me);
        Invoice inv = invoices.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        InvoicePayment pay = payments.findByIdAndInvoiceId(paymentId, id).orElseThrow(() -> ApiException.notFound("Payment not found."));
        if (pay.isReversed()) {
            throw ApiException.conflict("PAYMENT_ALREADY_REVERSED", "This payment has already been reversed.");
        }
        pay.reverse(me.id(), reason.trim());
        payments.flush();
        recompute(inv);
        messenger.toCompany(inv.getCompanyId(), null, "A payment of " + Money.format(pay.getAmount()) + " on invoice " + inv.getNumber()
                + " was reversed. The balance is now " + Money.format(inv.balance()) + ".", link(inv));
        audit.record(me, "Payment reversed", inv.getNumber() + " · " + Money.format(pay.getAmount()) + " · " + reason.trim());
        return toDtos(me, List.of(inv), true).get(0);
    }

    @Transactional
    public InvoiceDto voidInvoice(AuthUser me, UUID id, String reason) {
        ProjectService.requireManagement(me);
        Invoice inv = invoices.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        if (inv.getStatus() == Invoice.Status.VOID) {
            throw ApiException.conflict("INVALID_STATE", "This invoice is already void.");
        }
        if (!payments.findByInvoiceIdAndReversedFalse(id).isEmpty()) {
            throw ApiException.conflict("INVOICE_HAS_PAYMENTS", "This invoice has payments. Reverse them first.");
        }
        inv.markVoid(reason == null || reason.isBlank() ? null : reason.trim());
        if (inv.wasSent()) {
            messenger.toCompany(inv.getCompanyId(), null, "Invoice " + inv.getNumber() + " was cancelled. You do not need to pay it.", link(inv));
        }
        audit.record(me, "Invoice voided", inv.getNumber() + (inv.getVoidReason() == null ? "" : " · " + inv.getVoidReason()));
        return toDtos(me, List.of(inv), true).get(0);
    }

    @Transactional
    public InvoiceDto remind(AuthUser me, UUID id) {
        ProjectService.requireManagement(me);
        Invoice inv = invoices.findById(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        if (inv.getStatus() != Invoice.Status.SENT && inv.getStatus() != Invoice.Status.PARTIALLY_PAID) {
            throw ApiException.conflict("INVALID_STATE", "Only an invoice that is waiting for payment can be reminded.");
        }
        LocalDate today = LocalDate.now();
        messenger.toCompany(inv.getCompanyId(), "Payment reminder: invoice " + inv.getNumber(),
                "Reminder: invoice " + inv.getNumber() + " has a balance of " + Money.format(inv.balance()) + ", due " + inv.getDueDate()
                        + (inv.isOverdue(today) ? " (overdue)." : "."), link(inv));
        audit.record(me, "Invoice reminder sent", inv.getNumber() + " · " + names.company(inv.getCompanyId()));
        return toDtos(me, List.of(inv), true).get(0);
    }

    // ---------------------------------------------------------------- helpers

    private record Resolved(UUID projectId, UUID companyId) {
    }

    /** Works out the project and the company from the body. {@code fixedCompany} is set when editing: it cannot change. */
    private Resolved resolve(AuthUser me, InvoiceInput in, UUID fixedCompany) {
        UUID projectId = null;
        UUID companyId = in.companyId();
        if (in.projectId() != null) {
            Project p = projectService.visible(me, in.projectId());
            projectId = p.getId();
            if (companyId != null && !companyId.equals(p.getCompanyId())) {
                throw ApiException.invalidField("companyId", "This project belongs to a different client.");
            }
            companyId = p.getCompanyId();
        }
        if (companyId == null) {
            throw ApiException.invalidField("companyId", "Choose a project or a client.");
        }
        if (fixedCompany != null && !fixedCompany.equals(companyId)) {
            throw ApiException.invalidField("projectId", "The client of an invoice cannot be changed. Void it and create a new one.");
        }
        ClientCompany c = companies.findById(companyId).orElseThrow(() -> ApiException.notFound("Client not found."));
        return new Resolved(projectId, c.getId());
    }

    private static void checkDates(InvoiceInput in) {
        if (in.dueDate().isBefore(in.issueDate())) {
            throw ApiException.badRequest("DUE_BEFORE_ISSUE", "The due date cannot be before the issue date.");
        }
    }

    private void applyContent(Invoice inv, InvoiceInput in, UUID projectId) {
        long subtotal = 0;
        for (LineInput l : in.lines()) {
            subtotal += Money.lineTotal(l.quantity(), l.unitPrice());
        }
        BigDecimal rate = in.taxRate() == null ? BigDecimal.ZERO : in.taxRate();
        inv.setContent(projectId, in.issueDate(), in.dueDate(), rate, in.note() == null || in.note().isBlank() ? null : in.note().trim(),
                subtotal, Money.tax(subtotal, rate));
    }

    private void saveLines(Invoice inv, List<LineInput> inputs) {
        int order = 0;
        for (LineInput l : inputs) {
            lines.save(new InvoiceLine(inv.getId(), order++, l.description().trim(), l.quantity(), l.unitPrice(),
                    Money.lineTotal(l.quantity(), l.unitPrice())));
        }
    }

    /** INV-2026-0001. The upsert locks the year's counter row until commit, so numbers are unique and gap-free. */
    private String nextNumber(int year) {
        Integer n = jdbc.queryForObject("""
                insert into invoice_counter (year, last_number) values (?, 1)
                on conflict (year) do update set last_number = invoice_counter.last_number + 1
                returning last_number
                """, Integer.class, year);
        return String.format("INV-%d-%04d", year, n);
    }

    /** Sets paidAmount and the status from the payments that were not reversed. */
    private void recompute(Invoice inv) {
        long paid = payments.findByInvoiceIdAndReversedFalse(inv.getId()).stream().mapToLong(InvoicePayment::getAmount).sum();
        inv.setPaidAmount(paid);
        if (inv.getStatus() == Invoice.Status.SENT || inv.getStatus() == Invoice.Status.PARTIALLY_PAID || inv.getStatus() == Invoice.Status.PAID) {
            inv.setStatus(paid <= 0 ? Invoice.Status.SENT : paid >= inv.getTotal() ? Invoice.Status.PAID : Invoice.Status.PARTIALLY_PAID);
        }
    }

    static String link(Invoice inv) {
        return "/invoices/" + inv.getId();
    }

    /** The invoice, if this person may see it: not a developer, and a client only sees their own company's sent invoices. */
    private Invoice visible(AuthUser me, UUID id) {
        ProjectService.requireNotDeveloper(me);
        Invoice inv = invoices.findById(id).orElseThrow(() -> ApiException.notFound("Invoice not found."));
        if (me.isClient() && (!inv.getCompanyId().equals(me.companyId()) || !inv.wasSent())) {
            throw ApiException.notFound("Invoice not found.");
        }
        return inv;
    }

    private List<Invoice> select(AuthUser me, Filter f) {
        ProjectService.requireNotDeveloper(me);
        Params.checkRange(f.from(), f.to());
        Invoice.Status stored = null;
        boolean overdueOnly = false;
        if (f.status() != null && !f.status().isBlank()) {
            if (f.status().trim().equalsIgnoreCase("OVERDUE")) {
                overdueOnly = true;
            } else {
                stored = Params.enumValue(Invoice.Status.class, f.status(), "status");
            }
        }
        List<Invoice> base;
        if (me.isClient()) {
            base = me.companyId() == null ? List.of() : invoices.findByCompanyId(me.companyId()).stream().filter(Invoice::wasSent).toList();
        } else if (f.companyId() != null) {
            base = invoices.findByCompanyId(f.companyId());
        } else {
            base = invoices.findAll();
        }
        LocalDate today = LocalDate.now();
        Invoice.Status wanted = stored;
        boolean overdue = overdueOnly;
        return sorted(base.stream()
                .filter(i -> f.projectId() == null || f.projectId().equals(i.getProjectId()))
                .filter(i -> f.from() == null || !i.getIssueDate().isBefore(f.from()))
                .filter(i -> f.to() == null || !i.getIssueDate().isAfter(f.to()))
                .filter(i -> wanted == null || i.getStatus() == wanted)
                .filter(i -> !overdue || i.isOverdue(today))
                .collect(Collectors.toList()));
    }

    private static List<Invoice> sorted(List<Invoice> list) {
        return list.stream().sorted(Comparator.comparing(Invoice::getIssueDate).thenComparing(Invoice::getNumber).reversed()).toList();
    }

    List<InvoiceDto> toDtos(AuthUser me, List<Invoice> found, boolean withDetail) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = found.stream().map(Invoice::getId).toList();
        Map<UUID, List<InvoiceLine>> lineMap = withDetail
                ? lines.findByInvoiceIdInOrderBySortOrderAsc(ids).stream().collect(Collectors.groupingBy(InvoiceLine::getInvoiceId)) : Map.of();
        Map<UUID, List<InvoicePayment>> payMap = withDetail
                ? payments.findByInvoiceIdInOrderByRecordedAtAsc(ids).stream().collect(Collectors.groupingBy(InvoicePayment::getInvoiceId)) : Map.of();
        List<UUID> people = new ArrayList<>();
        found.forEach(i -> people.add(i.getCreatedBy()));
        payMap.values().forEach(l -> l.forEach(p -> {
            people.add(p.getRecordedBy());
            people.add(p.getReversedBy());
        }));
        Map<UUID, String> userNames = names.users(people);
        Map<UUID, String> companyNames = names.companies(found.stream().map(Invoice::getCompanyId).toList());
        Map<UUID, String> projectNames = projectNames(found);
        LocalDate today = LocalDate.now();
        List<InvoiceDto> out = new ArrayList<>();
        for (Invoice i : found) {
            boolean overdue = i.isOverdue(today);
            List<LineDto> lineDtos = null;
            List<PaymentDto> payDtos = null;
            if (withDetail) {
                lineDtos = lineMap.getOrDefault(i.getId(), List.of()).stream()
                        .map(l -> new LineDto(l.getId(), l.getDescription(), l.getQuantity(), l.getUnitPrice(), l.getLineTotal())).toList();
                payDtos = payMap.getOrDefault(i.getId(), List.of()).stream().map(p -> new PaymentDto(p.getId(), p.getAmount(), p.getPaidOn(),
                        p.getMethod(), p.getReference(), userNames.get(p.getRecordedBy()), p.getRecordedAt(), p.isReversed(), p.getReversedAt(),
                        me.isClient() ? null : userNames.get(p.getReversedBy()), me.isClient() ? null : p.getReverseReason())).toList();
            }
            out.add(new InvoiceDto(i.getId(), i.getNumber(), i.getProjectId(), projectNames.get(i.getProjectId()), i.getCompanyId(),
                    companyNames.get(i.getCompanyId()), i.getStatus(), overdue ? "OVERDUE" : i.getStatus().name(), overdue,
                    overdue ? ChronoUnit.DAYS.between(i.getDueDate(), today) : null, i.getIssueDate(), i.getDueDate(), i.getTaxRate(),
                    i.getSubtotal(), i.getTaxAmount(), i.getTotal(), i.getPaidAmount(), i.balance(), i.getNote(),
                    userNames.get(i.getCreatedBy()), i.getCreatedAt(), i.getSentAt(), i.getVoidedAt(),
                    me.isClient() ? null : i.getVoidReason(), lineDtos, payDtos));
        }
        return out;
    }

    private Map<UUID, String> projectNames(List<Invoice> found) {
        Map<UUID, String> out = new java.util.HashMap<>();
        List<UUID> ids = found.stream().map(Invoice::getProjectId).filter(java.util.Objects::nonNull).distinct().toList();
        if (!ids.isEmpty()) {
            projects.findAllById(ids).forEach(p -> out.put(p.getId(), p.getName()));
        }
        return out;
    }
}
