package com.itzetech.dpdms.renewal;

import com.itzetech.dpdms.renewal.RenewalDtos.CreateInput;
import com.itzetech.dpdms.renewal.RenewalDtos.RenewInput;
import com.itzetech.dpdms.renewal.RenewalDtos.RenewalDto;
import com.itzetech.dpdms.renewal.RenewalDtos.ReminderRun;
import com.itzetech.dpdms.renewal.RenewalDtos.RunRequest;
import com.itzetech.dpdms.renewal.RenewalDtos.UpdateInput;
import com.itzetech.dpdms.renewal.RenewalService.Filter;
import com.itzetech.dpdms.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
class RenewalController {

    private final RenewalService service;
    private final RenewalReminderService reminders;

    RenewalController(RenewalService service, RenewalReminderService reminders) {
        this.service = service;
        this.reminders = reminders;
    }

    @GetMapping("/api/renewals")
    List<RenewalDto> list(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String status,
                          @RequestParam(required = false) String type, @RequestParam(required = false) UUID companyId,
                          @RequestParam(required = false) UUID projectId,
                          @RequestParam(required = false) Integer expiringWithinDays) {
        if (expiringWithinDays != null && (expiringWithinDays < 0 || expiringWithinDays > 3650)) {
            throw com.itzetech.dpdms.common.ApiException.badRequest("BAD_REQUEST", "The value for \"expiringWithinDays\" is not valid.");
        }
        return service.list(me, new Filter(status, type, companyId, projectId, expiringWithinDays));
    }

    @GetMapping("/api/renewals/{id}")
    RenewalDto get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.detail(me, id);
    }

    @GetMapping("/api/projects/{projectId}/renewals")
    List<RenewalDto> forProject(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId) {
        return service.forProject(me, projectId);
    }

    @PostMapping("/api/renewals")
    @ResponseStatus(HttpStatus.CREATED)
    RenewalDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateInput in) {
        return service.create(me, in);
    }

    @PutMapping("/api/renewals/{id}")
    RenewalDto update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody UpdateInput in) {
        return service.update(me, id, in);
    }

    @PostMapping("/api/renewals/{id}/renew")
    RenewalDto renew(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody RenewInput in) {
        return service.renew(me, id, in);
    }

    @DeleteMapping("/api/renewals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.delete(me, id);
    }

    @PostMapping("/api/renewals/{id}/request-renewal")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestRenewal(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.requestRenewal(me, id);
    }

    /** Under /api/admin, so the security layer already limits it to administrators. */
    @PostMapping("/api/admin/renewals/run-reminders")
    ReminderRun runReminders(@AuthenticationPrincipal AuthUser me, @RequestBody(required = false) RunRequest body) {
        LocalDate today = body == null || body.today() == null ? LocalDate.now() : body.today();
        return reminders.run(today, me);
    }
}
