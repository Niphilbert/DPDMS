package com.itzetech.dpdms.audit;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** The audit trail. Administrator only (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/audit")
class AuditController {

    record AuditDto(String id, Instant occurredAt, String actorName, String action, String subject, String ref) {
    }

    private final AuditRepository repo;

    AuditController(AuditRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    List<AuditDto> list(@RequestParam(required = false) String q, @RequestParam(defaultValue = "100") int limit) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        return repo.search(like, PageRequest.of(0, Math.min(Math.max(limit, 1), 500))).stream()
                .map(a -> new AuditDto(a.getId().toString(), a.getOccurredAt(), a.getActorName(), a.getAction(), a.getSubject(), a.getRef()))
                .toList();
    }
}
