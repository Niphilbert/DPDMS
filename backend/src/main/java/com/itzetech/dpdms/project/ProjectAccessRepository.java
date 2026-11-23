package com.itzetech.dpdms.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectAccessRepository extends JpaRepository<ProjectAccess, UUID> {

    List<ProjectAccess> findByProjectIdOrderByGrantedAtAsc(UUID projectId);

    Optional<ProjectAccess> findByProjectIdAndUserId(UUID projectId, UUID userId);
}
