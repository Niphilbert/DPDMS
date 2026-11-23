package com.itzetech.dpdms.user;

import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** A short list of active staff, so a project manager can choose who to give a task to. Not the admin user list. */
@RestController
@RequestMapping("/api/staff")
class StaffController {

    record StaffDto(UUID id, String fullName, String email, Role role) {
    }

    private final UserAccountRepository users;

    StaffController(UserAccountRepository users) {
        this.users = users;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<StaffDto> list(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) Role role) {
        if (!me.isManagement()) {
            throw ApiException.forbidden("FORBIDDEN", "Only the project manager or an administrator can see this list.");
        }
        if (role == Role.CLIENT) {
            throw ApiException.badRequest("ROLE_NOT_STAFF", "Clients are not staff.");
        }
        List<Role> roles = role == null ? List.of(Role.ADMIN, Role.PROJECT_MANAGER, Role.DEVELOPER) : List.of(role);
        return roles.stream()
                .flatMap(r -> users.findByRoleAndStatus(r, UserStatus.ACTIVE).stream())
                .sorted(Comparator.comparing((UserAccount u) -> u.getFullName().toLowerCase()).thenComparing(UserAccount::getEmail))
                .map(u -> new StaffDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole()))
                .toList();
    }
}
