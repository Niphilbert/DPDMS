package com.itzetech.dpdms.user;

import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.user.UserAdminService.UserWithSecret;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Account management. Administrator only (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/users")
class AdminUserController {

    record CreateStaffRequest(
            @NotBlank(message = "Enter the full name.") @Size(max = 150) String fullName,
            @NotBlank(message = "Enter the work email.") @Email(message = "Enter a valid email address.") @Size(max = 254) String email,
            @NotNull(message = "Choose a role.") Role role) {
    }

    record ChangeRoleRequest(@NotNull(message = "Choose a role.") Role role) {
    }

    private final UserAdminService service;

    AdminUserController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    List<UserDto> list(@RequestParam(required = false) Role role, @RequestParam(required = false) String q) {
        return service.list(role, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserWithSecret create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateStaffRequest r) {
        return service.createStaff(me, r.fullName(), r.email(), r.role());
    }

    @PatchMapping("/{id}/role")
    UserDto changeRole(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest r) {
        return service.changeRole(me, id, r.role());
    }

    @PostMapping("/{id}/deactivate")
    UserDto deactivate(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.deactivate(me, id);
    }

    @PostMapping("/{id}/reactivate")
    UserDto reactivate(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.reactivate(me, id);
    }

    @PostMapping("/{id}/reset-password")
    UserWithSecret resetPassword(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.resetPassword(me, id);
    }
}
