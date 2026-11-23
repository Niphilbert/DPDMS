package com.itzetech.dpdms.ticket;

import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.ticket.TicketDtos.AssignInput;
import com.itzetech.dpdms.ticket.TicketDtos.CommentInput;
import com.itzetech.dpdms.ticket.TicketDtos.CreateInput;
import com.itzetech.dpdms.ticket.TicketDtos.PriorityInput;
import com.itzetech.dpdms.ticket.TicketDtos.SlaRequest;
import com.itzetech.dpdms.ticket.TicketDtos.SlaRun;
import com.itzetech.dpdms.ticket.TicketDtos.StatusInput;
import com.itzetech.dpdms.ticket.TicketDtos.TicketDto;
import com.itzetech.dpdms.ticket.TicketService.Filter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
class TicketController {

    private final TicketService service;
    private final TicketSlaService sla;

    TicketController(TicketService service, TicketSlaService sla) {
        this.service = service;
        this.sla = sla;
    }

    @GetMapping("/api/tickets")
    List<TicketDto> list(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String status,
                         @RequestParam(required = false) String priority, @RequestParam(required = false) UUID projectId,
                         @RequestParam(required = false) UUID companyId, @RequestParam(required = false) UUID assigneeId,
                         @RequestParam(defaultValue = "false") boolean unassigned, @RequestParam(defaultValue = "false") boolean breached,
                         @RequestParam(defaultValue = "false") boolean mine) {
        return service.list(me, new Filter(status, priority, projectId, companyId, assigneeId, unassigned, breached, mine));
    }

    @GetMapping("/api/tickets/{id}")
    TicketDto get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.detail(me, id);
    }

    @PostMapping("/api/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    TicketDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateInput in) {
        return service.create(me, in);
    }

    @PostMapping("/api/tickets/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    TicketDto comment(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody CommentInput in) {
        return service.comment(me, id, in);
    }

    @PostMapping("/api/tickets/{id}/status")
    TicketDto status(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody StatusInput in) {
        return service.changeStatus(me, id, in.status());
    }

    @PostMapping("/api/tickets/{id}/assign")
    TicketDto assign(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody AssignInput in) {
        return service.assign(me, id, in);
    }

    @PostMapping("/api/tickets/{id}/priority")
    TicketDto priority(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody PriorityInput in) {
        return service.changePriority(me, id, in.priority());
    }

    @PostMapping("/api/tickets/{id}/reopen")
    TicketDto reopen(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.reopen(me, id);
    }

    @PostMapping("/api/tickets/{id}/close")
    TicketDto close(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.close(me, id);
    }

    /** Under /api/admin, so the security layer already limits it to administrators. */
    @PostMapping("/api/admin/tickets/run-sla-check")
    SlaRun runSlaCheck(@AuthenticationPrincipal AuthUser me, @RequestBody(required = false) SlaRequest body) {
        Instant now = body == null ? Instant.now()
                : body.now() != null ? body.now()
                : body.today() != null ? body.today().atStartOfDay().toInstant(ZoneOffset.UTC) : Instant.now();
        return sla.run(now, me);
    }
}
