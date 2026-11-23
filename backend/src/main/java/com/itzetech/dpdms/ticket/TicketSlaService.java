package com.itzetech.dpdms.ticket;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.notification.Messenger;
import com.itzetech.dpdms.project.Names;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.ticket.TicketDtos.SlaAlert;
import com.itzetech.dpdms.ticket.TicketDtos.SlaRun;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Watches the answer deadlines. A ticket that has used 75% of its time gets one "at risk" alert, and one "breached"
 * alert once the deadline has passed. The flag columns on the ticket are claimed with a conditional update, so each
 * alert goes out once even if the scheduled job and the admin endpoint run together.
 */
@Service
public class TicketSlaService {

    private final TicketRepository tickets;
    private final UserAccountRepository users;
    private final Messenger messenger;
    private final Names names;
    private final AuditService audit;

    public TicketSlaService(TicketRepository tickets, UserAccountRepository users, Messenger messenger, Names names, AuditService audit) {
        this.tickets = tickets;
        this.users = users;
        this.messenger = messenger;
        this.names = names;
        this.audit = audit;
    }

    /** @param actor who started it by hand, or {@code null} for the scheduled job */
    @Transactional
    public SlaRun run(Instant now, AuthUser actor) {
        List<Ticket> active = tickets.findByStatusIn(Ticket.ACTIVE).stream().sorted(Comparator.comparing(Ticket::getDueAt)).toList();
        List<SlaAlert> alerts = new ArrayList<>();
        int atRisk = 0, breached = 0;
        for (Ticket t : active) {
            String kind;
            if (now.isAfter(t.getDueAt())) {
                if (t.getSlaBreachNotifiedAt() != null || tickets.claimBreach(t.getId(), now) != 1) {
                    continue;
                }
                kind = "BREACHED";
                breached++;
            } else {
                long window = t.getDueAt().toEpochMilli() - t.getSlaStartedAt().toEpochMilli();
                long left = t.getDueAt().toEpochMilli() - now.toEpochMilli();
                if (left * 4 > window || t.getSlaWarnedAt() != null || tickets.claimWarning(t.getId(), now) != 1) {
                    continue;
                }
                kind = "AT_RISK";
                atRisk++;
            }
            UserAccount assignee = t.getAssigneeId() == null ? null : users.findById(t.getAssigneeId()).filter(UserAccount::isActive).orElse(null);
            List<UserAccount> to = new ArrayList<>(messenger.management());
            if (assignee != null) {
                to.add(assignee);
            }
            String who = names.company(t.getCompanyId());
            String message = kind.equals("BREACHED")
                    ? "Ticket " + t.getNumber() + " (" + t.getSubject() + ", " + who + ") has missed its answer deadline."
                    : "Ticket " + t.getNumber() + " (" + t.getSubject() + ", " + who + ") is close to its answer deadline: about "
                    + Math.max(0, left(t, now)) + " minutes left.";
            messenger.tell(to, "Ticket alert: " + t.getNumber(), message, TicketService.link(t));
            String subject = t.getNumber() + " · " + kind.toLowerCase().replace('_', ' ') + " · " + t.getSubject();
            if (actor == null) {
                audit.record(null, "System", "Ticket SLA alert", subject);
            } else {
                audit.record(actor, "Ticket SLA alert", subject);
            }
            alerts.add(new SlaAlert(t.getId(), t.getNumber(), kind, assignee == null ? null : assignee.getFullName()));
        }
        return new SlaRun(now, active.size(), atRisk, breached, alerts);
    }

    private static long left(Ticket t, Instant now) {
        return Math.floorDiv(t.getDueAt().toEpochMilli() - now.toEpochMilli(), 60_000L);
    }
}
