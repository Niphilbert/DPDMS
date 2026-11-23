package com.itzetech.dpdms.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, UUID> {

    List<ProjectTask> findByProjectIdOrderByPhaseAscSortOrderAscCreatedAtAsc(UUID projectId);

    List<ProjectTask> findByProjectIdIn(Collection<UUID> projectIds);

    Optional<ProjectTask> findByIdAndProjectId(UUID id, UUID projectId);

    List<ProjectTask> findByAssigneeIdOrderByDueDateAsc(UUID assigneeId);

    @Query("select coalesce(max(t.sortOrder), 0) from ProjectTask t where t.projectId = :projectId and t.phase = :phase")
    int maxSortOrder(@Param("projectId") UUID projectId, @Param("phase") int phase);
}
