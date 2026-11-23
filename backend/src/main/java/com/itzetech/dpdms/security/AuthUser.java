package com.itzetech.dpdms.security;

import com.itzetech.dpdms.user.Role;

import java.util.UUID;

/** The signed-in person, available in controllers with {@code @AuthenticationPrincipal}. */
public record AuthUser(UUID id, String email, String fullName, Role role, UUID companyId) {

    public boolean isStaff() {
        return role != Role.CLIENT;
    }

    /** Administrators and project managers: they see and run everything in delivery. */
    public boolean isManagement() {
        return role == Role.ADMIN || role == Role.PROJECT_MANAGER;
    }

    public boolean isDeveloper() {
        return role == Role.DEVELOPER;
    }

    public boolean isClient() {
        return role == Role.CLIENT;
    }
}
