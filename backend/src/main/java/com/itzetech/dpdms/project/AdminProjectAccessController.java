package com.itzetech.dpdms.project;

import com.itzetech.dpdms.project.ProjectDtos.AccessDto;
import com.itzetech.dpdms.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Lets the administrator show a project to a developer who has no task on it. Administrator only (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/projects/{projectId}/access")
class AdminProjectAccessController {

    record GrantRequest(@NotNull(message = "Choose a developer.") UUID userId) {
    }

    private final ProjectService projects;

    AdminProjectAccessController(ProjectService projects) {
        this.projects = projects;
    }

    @GetMapping
    List<AccessDto> list(@PathVariable UUID projectId) {
        return projects.accessList(projectId);
    }

    @PostMapping
    List<AccessDto> grant(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId, @Valid @RequestBody GrantRequest r) {
        return projects.grantAccess(me, projectId, r.userId());
    }

    @DeleteMapping("/{userId}")
    List<AccessDto> revoke(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId, @PathVariable UUID userId) {
        return projects.revokeAccess(me, projectId, userId);
    }
}
