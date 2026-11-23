package com.itzetech.dpdms.user;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String fullName,
        Role role,
        String roleLabel,
        UserStatus status,
        UUID companyId,
        String companyName,
        boolean selfRegistered,
        Instant lastLoginAt,
        Instant createdAt) {

    public static UserDto of(UserAccount u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getRole().label(), u.getStatus(),
                u.companyId(), u.getCompany() == null ? null : u.getCompany().getName(),
                u.getRole() == Role.CLIENT && u.getCreatedBy() == null, u.getLastLoginAt(), u.getCreatedAt());
    }
}
