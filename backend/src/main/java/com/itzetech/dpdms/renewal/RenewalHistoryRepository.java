package com.itzetech.dpdms.renewal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RenewalHistoryRepository extends JpaRepository<RenewalHistory, UUID> {

    List<RenewalHistory> findByRenewalIdInOrderByCreatedAtDesc(Collection<UUID> renewalIds);

    boolean existsByRenewalId(UUID renewalId);
}
