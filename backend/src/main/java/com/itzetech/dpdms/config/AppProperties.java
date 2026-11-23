package com.itzetech.dpdms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        List<String> corsAllowedOrigins,
        String mailFrom,
        Jwt jwt,
        BootstrapAdmin bootstrapAdmin,
        boolean exposeSecrets,
        long hourlyRate,
        Jobs jobs) {

    public record Jwt(String secret, int accessTokenMinutes) {
    }

    public record BootstrapAdmin(String email, String fullName, String password) {
    }

    /** The scheduled jobs: renewal reminders and the support-ticket deadline check. */
    public record Jobs(boolean enabled, String renewalRemindersCron, String ticketSlaCron) {
    }
}
