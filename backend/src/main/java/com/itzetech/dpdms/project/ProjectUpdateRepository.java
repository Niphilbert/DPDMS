package com.itzetech.dpdms.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, UUID> {

    List<ProjectUpdate> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
