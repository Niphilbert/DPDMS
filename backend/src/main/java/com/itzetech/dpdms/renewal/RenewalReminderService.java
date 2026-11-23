package com.itzetech.dpdms.renewal;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.notification.Messenger;
import com.itzetech.dpdms.project.Names;
import com.itzetech.dpdms.project.Project;
import com.itzetech.dpdms.project.ProjectRepository;
import com.itzetech.dpdms.renewal.RenewalDtos.ReminderItem;
import com.itzetech.dpdms.renewal.RenewalDtos.ReminderRun;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends the "your domain expires in N days" reminders: 60, 30, 14 and 7 days before, and once on expiry.
 * Exactly once per renewal, threshold and expiry date: the row in {@code renewal_reminder} (unique key) is the proof,
 * so the daily job and the admin endpoint can never both send the same reminder.
 */
@Service
public class RenewalReminderService {

    /** Days before expiry, largest first. 0 is "on expiry (or later)". */
    static final int[] THRESHOLDS = {60, 30, 14, 7, 0};

    private final RenewalRepository renewals;
    private final ProjectRepository projects;
    private final Messenger messenger;
    private final Names names;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public RenewalReminderService(RenewalRepository renewals, ProjectRepository projects, Messenger messenger, Names names,
                                  AuditService audit, JdbcTemplate jdbc) {
        this.renewals = renewals;
        this.projects = projects;
        this.messenger = messenger;
        this.names = names;
        this.audit = audit;
        this.jdbc = jdbc;
    }

    /** The nearest threshold that has been reached, or -1 when the expiry is still more than 60 days away. */
    static int thresholdFor(long daysLeft) {
        int reached = -1;
        for (int t : THRESHOLDS) {
            if (daysLeft <= t) {
                reached = t;
            }
        }
        return reached;
    }

    /** @param actor who started it by hand, or {@code null} for the scheduled job */
    @Transactional
    public ReminderRun run(LocalDate today, AuthUser actor) {
        List<Renewal> due = renewals.findByExpiresOnLessThanEqual(today.plusDays(THRESHOLDS[0])).stream()
                .sorted(Comparator.comparing(Renewal::getExpiresOn).thenComparing(Renewal::getLabel)).toList();
        Map<UUID, String> projectNames = new HashMap<>();
        if (!due.isEmpty()) {
            for (Project p : projects.findAllById(due.stream().map(Renewal::getProjectId).distinct().toList())) {
                projectNames.put(p.getId(), p.getName());
            }
        }
        List<ReminderItem> sent = new ArrayList<>();
        for (Renewal r : due) {
            long daysLeft = r.daysLeft(today);
            int threshold = thresholdFor(daysLeft);
            if (threshold < 0 || !claim(r, threshold)) {
                continue;
            }
            String project = projectNames.getOrDefault(r.getProjectId(), "your project");
            String what = r.getType().label().toLowerCase() + " " + r.getLabel();
            String when = daysLeft > 1 ? "expires in " + daysLeft + " days, on " + r.getExpiresOn()
                    : daysLeft == 1 ? "expires tomorrow, on " + r.getExpiresOn()
                    : daysLeft == 0 ? "expires today"
                    : "expired on " + r.getExpiresOn();
            messenger.toCompanyAndManagement(r.getCompanyId(), "Renewal reminder: " + r.getLabel(),
                    "Reminder: the " + what + " for " + project + " " + when + ". " + (daysLeft >= 0 ? "Please renew it in time." : "Please renew it now."),
                    RenewalService.link(r));
            String subject = RenewalService.describe(r) + " · " + (daysLeft < 0 ? "expired" : daysLeft + " days left") + " · " + names.company(r.getCompanyId());
            if (actor == null) {
                audit.record(null, "System", "Renewal reminder sent", subject);
            } else {
                audit.record(actor, "Renewal reminder sent", subject);
            }
            sent.add(new ReminderItem(r.getId(), r.getType(), r.getLabel(), project, r.getExpiresOn(), daysLeft, threshold));
        }
        return new ReminderRun(today, due.size(), sent.size(), sent);
    }

    /** Records that this reminder is being sent. False when it was already sent (or is being sent by someone else right now). */
    private boolean claim(Renewal r, int threshold) {
        int inserted = jdbc.update("""
                insert into renewal_reminder (id, renewal_id, expires_on, threshold_days, sent_at) values (?, ?, ?, ?, now())
                on conflict (renewal_id, expires_on, threshold_days) do nothing
                """, UUID.randomUUID(), r.getId(), r.getExpiresOn(), threshold);
        return inserted == 1;
    }
}
