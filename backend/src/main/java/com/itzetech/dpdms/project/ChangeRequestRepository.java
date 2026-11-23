package com.itzetech.dpdms.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {

    List<ChangeRequest> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ChangeRequest> findByIdAndProjectId(UUID id, UUID projectId);
}
