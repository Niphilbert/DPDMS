package com.itzetech.dpdms.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditRepository extends JpaRepository<AuditEntry, UUID> {

    @Query("""
            select a from AuditEntry a
            where :q is null or lower(a.actorName) like :q or lower(a.action) like :q or lower(coalesce(a.subject, '')) like :q
            order by a.occurredAt desc
            """)
    List<AuditEntry> search(@Param("q") String q, Pageable page);
}
