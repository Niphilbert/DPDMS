package com.itzetech.dpdms.notification;

import com.itzetech.dpdms.user.Role;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import com.itzetech.dpdms.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Creates in-app notifications. Other modules call this when something needs a person's attention. */
@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final UserAccountRepository users;

    public NotificationService(NotificationRepository repo, UserAccountRepository users) {
        this.repo = repo;
        this.users = users;
    }

    @Transactional
    public void notifyUser(UUID userId, String message, String link) {
        repo.save(new Notification(userId, message, link));
    }

    /** Everyone with this role who can sign in. */
    @Transactional
    public void notifyRole(Role role, String message, String link) {
        users.findByRoleAndStatus(role, UserStatus.ACTIVE).forEach(u -> notifyUser(u.getId(), message, link));
    }

    /** Every active person at a client company. */
    @Transactional
    public void notifyCompany(UUID companyId, String message, String link) {
        users.findByCompanyIdAndStatus(companyId, UserStatus.ACTIVE).forEach(u -> notifyUser(u.getId(), message, link));
    }

    public void notifyUser(UserAccount user, String message, String link) {
        notifyUser(user.getId(), message, link);
    }
}
