package com.itzetech.dpdms.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    List<Approval> findByProjectIdOrderBySentAtDesc(UUID projectId);

    Optional<Approval> findByIdAndProjectId(UUID id, UUID projectId);

    int countByProjectIdAndStatus(UUID projectId, Approval.Status status);

    @Query("select coalesce(max(a.version), 0) from Approval a where a.projectId = :projectId and a.itemKey = :itemKey")
    int latestVersion(@Param("projectId") UUID projectId, @Param("itemKey") String itemKey);
}
