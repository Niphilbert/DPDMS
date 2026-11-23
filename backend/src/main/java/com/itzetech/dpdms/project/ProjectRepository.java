package com.itzetech.dpdms.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    /** Projects a developer may see: one with a task assigned to them, or one the administrator granted. */
    @Query("""
            select p from Project p
            where p.id in (select t.projectId from ProjectTask t where t.assigneeId = :userId)
               or p.id in (select a.projectId from ProjectAccess a where a.userId = :userId)
            order by p.createdAt desc
            """)
    List<Project> findVisibleToDeveloper(@Param("userId") UUID userId);

    @Query("""
            select count(p) > 0 from Project p
            where p.id = :projectId
              and (p.id in (select t.projectId from ProjectTask t where t.assigneeId = :userId)
                   or p.id in (select a.projectId from ProjectAccess a where a.userId = :userId))
            """)
    boolean isVisibleToDeveloper(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
