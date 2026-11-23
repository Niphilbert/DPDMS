package com.itzetech.dpdms.notification;

import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    record NotificationDto(UUID id, String message, String link, Instant createdAt, boolean read) {
        static NotificationDto of(Notification n) {
            return new NotificationDto(n.getId(), n.getMessage(), n.getLink(), n.getCreatedAt(), n.getReadAt() != null);
        }
    }

    record NotificationList(long unread, List<NotificationDto> items) {
    }

    private final NotificationRepository repo;

    NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    NotificationList mine(@AuthenticationPrincipal AuthUser me) {
        List<NotificationDto> items = repo.findByUserIdOrderByCreatedAtDesc(me.id(), PageRequest.of(0, 50))
                .stream().map(NotificationDto::of).toList();
        return new NotificationList(repo.countByUserIdAndReadAtIsNull(me.id()), items);
    }

    @PostMapping("/{id}/read")
    @Transactional
    NotificationDto read(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        Notification n = repo.findByIdAndUserId(id, me.id()).orElseThrow(() -> ApiException.notFound("Notification not found."));
        n.markRead();
        return NotificationDto.of(n);
    }

    @PostMapping("/read-all")
    @Transactional
    NotificationList readAll(@AuthenticationPrincipal AuthUser me) {
        repo.markAllRead(me.id(), Instant.now());
        return mine(me);
    }
}
