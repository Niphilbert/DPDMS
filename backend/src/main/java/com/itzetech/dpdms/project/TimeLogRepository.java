package com.itzetech.dpdms.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {

    List<TimeLog> findByTaskIdOrderByWorkDateDescCreatedAtDesc(UUID taskId);

    List<TimeLog> findByTaskIdInOrderByWorkDateDescCreatedAtDesc(Collection<UUID> taskIds);
}
