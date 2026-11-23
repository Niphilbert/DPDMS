package com.itzetech.dpdms.report;

import com.itzetech.dpdms.invoice.Invoice;
import com.itzetech.dpdms.invoice.InvoiceRepository;
import com.itzetech.dpdms.renewal.Renewal;
import com.itzetech.dpdms.renewal.RenewalService;
import com.itzetech.dpdms.report.ReportDtos.Dashboard;
import com.itzetech.dpdms.report.ReportDtos.InvoiceCounts;
import com.itzetech.dpdms.report.ReportDtos.RenewalCounts;
import com.itzetech.dpdms.report.ReportDtos.TicketCounts;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.ticket.Ticket;
import com.itzetech.dpdms.ticket.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** The stage 3 counts for the home page. Each person only gets the blocks they may see. */
@Service
public class DashboardService {

    private final InvoiceRepository invoices;
    private final TicketRepository tickets;
    private final RenewalService renewalService;

    public DashboardService(InvoiceRepository invoices, TicketRepository tickets, RenewalService renewalService) {
        this.invoices = invoices;
        this.tickets = tickets;
        this.renewalService = renewalService;
    }

    @Transactional(readOnly = true)
    public Dashboard forUser(AuthUser me) {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        InvoiceCounts invoiceCounts = null;
        if (!me.isDeveloper()) {
            List<Invoice> mine = me.isClient()
                    ? (me.companyId() == null ? List.<Invoice>of() : invoices.findByCompanyId(me.companyId()).stream().filter(Invoice::wasSent).toList())
                    : invoices.findAll();
            List<Invoice> unpaid = mine.stream().filter(i -> i.getStatus() == Invoice.Status.SENT || i.getStatus() == Invoice.Status.PARTIALLY_PAID).toList();
            List<Invoice> overdue = unpaid.stream().filter(i -> i.isOverdue(today)).toList();
            invoiceCounts = new InvoiceCounts(unpaid.size(), unpaid.stream().mapToLong(Invoice::balance).sum(), overdue.size(),
                    overdue.stream().mapToLong(Invoice::balance).sum(),
                    me.isManagement() ? (int) mine.stream().filter(i -> i.getStatus() == Invoice.Status.DRAFT).count() : null);
        }

        List<Renewal> rs = renewalService.scope(me);
        RenewalCounts renewalCounts = new RenewalCounts((int) rs.stream().filter(r -> r.status(today) == Renewal.Status.EXPIRING).count(),
                (int) rs.stream().filter(r -> r.status(today) == Renewal.Status.EXPIRED).count());

        List<Ticket> all = switch (me.role()) {
            case ADMIN, PROJECT_MANAGER -> tickets.findAll();
            case CLIENT -> me.companyId() == null ? List.<Ticket>of() : tickets.findByCompanyId(me.companyId());
            case DEVELOPER -> tickets.findByAssigneeId(me.id());
        };
        List<Ticket> open = all.stream().filter(Ticket::isActive).toList();
        TicketCounts ticketCounts = new TicketCounts(open.size(),
                me.isClient() ? null : (Integer) (int) open.stream().filter(t -> t.isBreached(now)).count(),
                me.isClient() ? null : (Integer) (int) open.stream().filter(t -> t.slaState(now) == Ticket.SlaState.AT_RISK).count(),
                me.isManagement() ? (Integer) (int) open.stream().filter(t -> t.getAssigneeId() == null).count() : null,
                me.isDeveloper() ? null : (Integer) (int) open.stream().filter(t -> t.getStatus() == Ticket.Status.WAITING_CLIENT).count());
        return new Dashboard(invoiceCounts, renewalCounts, ticketCounts);
    }
}
