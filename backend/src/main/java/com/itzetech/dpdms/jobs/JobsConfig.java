package com.itzetech.dpdms.jobs;

import com.itzetech.dpdms.renewal.RenewalReminderService;
import com.itzetech.dpdms.ticket.TicketSlaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The scheduled jobs. Switched on by {@code app.jobs.enabled} (default true; the tests turn it off). The same logic can be run by
 * hand through {@code POST /api/admin/renewals/run-reminders} and {@code POST /api/admin/tickets/run-sla-check}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
class JobsConfig {

    private static final Logger log = LoggerFactory.getLogger(JobsConfig.class);

    private final RenewalReminderService renewals;
    private final TicketSlaService tickets;

    JobsConfig(RenewalReminderService renewals, TicketSlaService tickets) {
        this.renewals = renewals;
        this.tickets = tickets;
    }

    @Scheduled(cron = "${app.jobs.renewal-reminders-cron}")
    void renewalReminders() {
        try {
            var run = renewals.run(LocalDate.now(), null);
            log.info("Renewal reminders: {} checked, {} sent.", run.checked(), run.sent());
        } catch (RuntimeException e) {
            log.error("The renewal reminder job failed", e);
        }
    }

    @Scheduled(cron = "${app.jobs.ticket-sla-cron}")
    void ticketDeadlines() {
        try {
            var run = tickets.run(Instant.now(), null);
            if (run.atRisk() + run.breached() > 0) {
                log.info("Ticket deadlines: {} checked, {} at risk, {} breached.", run.checked(), run.atRisk(), run.breached());
            }
        } catch (RuntimeException e) {
            log.error("The ticket deadline job failed", e);
        }
    }
}
