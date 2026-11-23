package com.itzetech.dpdms.notification;

import com.itzetech.dpdms.config.AppProperties;
import com.itzetech.dpdms.mail.MailService;
import com.itzetech.dpdms.user.Role;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import com.itzetech.dpdms.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tells groups of people about something: an in-app notification for each, and optionally an e-mail.
 * A person who is in several groups (for example a manager) is told once. A failing mail server never breaks the action.
 */
@Service
public class Messenger {

    private static final Logger log = LoggerFactory.getLogger(Messenger.class);

    private final NotificationService notifications;
    private final UserAccountRepository users;
    private final MailService mail;
    private final AppProperties props;

    public Messenger(NotificationService notifications, UserAccountRepository users, MailService mail, AppProperties props) {
        this.notifications = notifications;
        this.users = users;
        this.mail = mail;
        this.props = props;
    }

    /** Active administrators and project managers. */
    public List<UserAccount> management() {
        List<UserAccount> out = new java.util.ArrayList<>(users.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE));
        out.addAll(users.findByRoleAndStatus(Role.PROJECT_MANAGER, UserStatus.ACTIVE));
        return out;
    }

    public List<UserAccount> company(UUID companyId) {
        return companyId == null ? List.of() : users.findByCompanyIdAndStatus(companyId, UserStatus.ACTIVE);
    }

    /** Notifies everyone in the three groups once. Pass {@code subject} to also send an e-mail, or {@code null} for in-app only. */
    @Transactional
    public void tell(Collection<UserAccount> recipients, String subject, String message, String link) {
        Map<UUID, UserAccount> unique = new LinkedHashMap<>();
        recipients.forEach(u -> unique.putIfAbsent(u.getId(), u));
        for (UserAccount u : unique.values()) {
            notifications.notifyUser(u.getId(), message, link);
            if (subject != null) {
                try {
                    mail.send(u.getEmail(), subject, "Hello " + u.getFullName() + ",\n\n" + message
                            + "\n\nOpen it here: " + props.frontendUrl() + (link == null ? "" : link) + "\n\nItzetech");
                } catch (RuntimeException e) {
                    log.warn("Could not e-mail {}: {}", u.getEmail(), e.getMessage());
                }
            }
        }
    }

    public void toCompany(UUID companyId, String subject, String message, String link) {
        tell(company(companyId), subject, message, link);
    }

    public void toManagement(String subject, String message, String link) {
        tell(management(), subject, message, link);
    }

    public void toCompanyAndManagement(UUID companyId, String subject, String message, String link) {
        List<UserAccount> all = new java.util.ArrayList<>(company(companyId));
        all.addAll(management());
        tell(all, subject, message, link);
    }
}
