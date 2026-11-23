package com.itzetech.dpdms.renewal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RenewalRepository extends JpaRepository<Renewal, UUID> {

    List<Renewal> findByCompanyId(UUID companyId);

    List<Renewal> findByProjectId(UUID projectId);

    List<Renewal> findByProjectIdIn(Collection<UUID> projectIds);

    /** Renewals that are expired or expire on or before the given day. */
    List<Renewal> findByExpiresOnLessThanEqual(LocalDate day);
}
