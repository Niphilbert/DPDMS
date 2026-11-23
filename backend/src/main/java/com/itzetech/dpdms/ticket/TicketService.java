package com.itzetech.dpdms.ticket;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.common.Params;
import com.itzetech.dpdms.notification.Messenger;
import com.itzetech.dpdms.project.Names;
import com.itzetech.dpdms.project.Project;
import com.itzetech.dpdms.project.ProjectRepository;
import com.itzetech.dpdms.project.ProjectService;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.ticket.TicketDtos.AssignInput;
import com.itzetech.dpdms.ticket.TicketDtos.CommentDto;
import com.itzetech.dpdms.ticket.TicketDtos.CommentInput;
import com.itzetech.dpdms.ticket.TicketDtos.CreateInput;
import com.itzetech.dpdms.ticket.TicketDtos.TicketDto;
import com.itzetech.dpdms.user.Role;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Support tickets: raising, working on, assigning and closing them, with an answer deadline per priority. */
@Service
public class TicketService {

    /** What staff may move a ticket to. CLOSED is handled separately (management, from RESOLVED). */
    private static final Map<Ticket.Status, Set<Ticket.Status>> STAFF_MOVES = new EnumMap<>(Ticket.Status.class);

    static {
        STAFF_MOVES.put(Ticket.Status.OPEN, EnumSet.of(Ticket.Status.IN_PROGRESS, Ticket.Status.WAITING_CLIENT, Ticket.Status.RESOLVED));
        STAFF_MOVES.put(Ticket.Status.IN_PROGRESS, EnumSet.of(Ticket.Status.WAITING_CLIENT, Ticket.Status.RESOLVED));
        STAFF_MOVES.put(Ticket.Status.WAITING_CLIENT, EnumSet.of(Ticket.Status.IN_PROGRESS, Ticket.Status.RESOLVED));
        STAFF_MOVES.put(Ticket.Status.RESOLVED, EnumSet.of(Ticket.Status.IN_PROGRESS, Ticket.Status.CLOSED));
        STAFF_MOVES.put(Ticket.Status.CLOSED, EnumSet.noneOf(Ticket.Status.class));
    }

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final ProjectRepository projects;
    private final UserAccountRepository users;
    private final Names names;
    private final Messenger messenger;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public TicketService(TicketRepository tickets, TicketCommentRepository comments, ProjectRepository projects,
                         UserAccountRepository users, Names names, Messenger messenger, AuditService audit, JdbcTemplate jdbc) {
        this.tickets = tickets;
        this.comments = comments;
        this.projects = projects;
        this.users = users;
        this.names = names;
        this.messenger = messenger;
        this.audit = audit;
        this.jdbc = jdbc;
    }

    /** The optional filters of the list. */
    public record Filter(String status, String priority, UUID projectId, UUID companyId, UUID assigneeId,
                         boolean unassigned, boolean breached, boolean mine) {
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<TicketDto> list(AuthUser me, Filter f) {
        Ticket.Status status = Params.enumValue(Ticket.Status.class, f.status(), "status");
        Ticket.Priority priority = Params.enumValue(Ticket.Priority.class, f.priority(), "priority");
        Instant now = Instant.now();
        List<Ticket> base = switch (me.role()) {
            case ADMIN, PROJECT_MANAGER -> tickets.findAll();
            case CLIENT -> me.companyId() == null ? List.<Ticket>of() : tickets.findByCompanyId(me.companyId());
            case DEVELOPER -> tickets.findByAssigneeId(me.id());
        };
        boolean mgmt = me.isManagement();
        List<Ticket> found = base.stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> f.projectId() == null || f.projectId().equals(t.getProjectId()))
                .filter(t -> !mgmt || f.companyId() == null || t.getCompanyId().equals(f.companyId()))
                .filter(t -> !mgmt || f.assigneeId() == null || f.assigneeId().equals(t.getAssigneeId()))
                .filter(t -> !mgmt || !f.unassigned() || t.getAssigneeId() == null)
                .filter(t -> !f.breached() || t.isBreached(now))
                .filter(t -> !f.mine() || me.id().equals(t.getAssigneeId()))
                .sorted(Comparator.comparingInt((Ticket t) -> t.isActive() ? 0 : 1)
                        .thenComparingLong(t -> t.isActive() ? t.getDueAt().toEpochMilli() : -t.getCreatedAt().toEpochMilli()))
                .toList();
        return toDtos(me, found, false, now);
    }

    @Transactional(readOnly = true)
    public TicketDto detail(AuthUser me, UUID id) {
        return toDtos(me, List.of(visible(me, id)), true, Instant.now()).get(0);
    }

    // ---------------------------------------------------------------- client actions

    @Transactional
    public TicketDto create(AuthUser me, CreateInput in) {
        if (!me.isClient()) {
            throw ApiException.forbidden("CLIENT_ONLY", "Only a client can raise a support ticket.");
        }
        UUID projectId = null;
        if (in.projectId() != null) {
            Project p = projects.findById(in.projectId()).filter(x -> x.getCompanyId().equals(me.companyId()))
                    .orElseThrow(() -> ApiException.notFound("Project not found."));
            projectId = p.getId();
        }
        Long seq = jdbc.queryForObject("select nextval('ticket_number_seq')", Long.class);
        Instant now = Instant.now();
        Ticket t = tickets.save(new Ticket(String.format("TCK-%04d", seq), me.companyId(), projectId, in.subject().trim(),
                in.description().trim(), in.priority(), me.id(), now));
        messenger.toManagement(null, "New " + t.getPriority().name().toLowerCase() + " support ticket " + t.getNumber() + " from "
                + names.company(t.getCompanyId()) + ": " + t.getSubject() + ". Answer due in " + t.getSlaHours() + " hours.", link(t));
        audit.record(me, "Ticket opened", t.getNumber() + " · " + t.getSubject() + " · " + t.getPriority());
        return toDtos(me, List.of(t), true, now).get(0);
    }

    @Transactional
    public TicketDto reopen(AuthUser me, UUID id) {
        requireClient(me);
        Ticket t = visible(me, id);
        if (t.getStatus() != Ticket.Status.RESOLVED) {
            throw ApiException.conflict("INVALID_TRANSITION", "Only a resolved ticket can be reopened.");
        }
        if (t.getReopenCount() >= 1) {
            throw ApiException.conflict("REOPEN_LIMIT", "This ticket has already been reopened once. Please raise a new ticket.");
        }
        Instant now = Instant.now();
        t.reopen(now, true);
        tellStaffSide(t, me.id(), me.fullName() + " reopened ticket " + t.getNumber() + ": " + t.getSubject() + ".");
        audit.record(me, "Ticket reopened", t.getNumber() + " · " + t.getSubject());
        return toDtos(me, List.of(t), true, now).get(0);
    }

    // ---------------------------------------------------------------- shared actions

    @Transactional
    public TicketDto comment(AuthUser me, UUID id, CommentInput in) {
        Ticket t = visible(me, id);
        boolean internal = Boolean.TRUE.equals(in.internal());
        if (me.isClient() && internal) {
            throw ApiException.forbidden("FORBIDDEN", "Internal notes are for staff only.");
        }
        if (t.getStatus() == Ticket.Status.CLOSED) {
            throw ApiException.conflict("TICKET_CLOSED", "This ticket is closed.");
        }
        Instant now = Instant.now();
        comments.save(new TicketComment(t.getId(), me.id(), in.body().trim(), internal, now));
        t.touch(now);
        if (me.isClient()) {
            if (t.getStatus() == Ticket.Status.WAITING_CLIENT) {
                t.moveTo(Ticket.Status.IN_PROGRESS, now);
            }
            tellStaffSide(t, me.id(), me.fullName() + " replied on ticket " + t.getNumber() + ": " + t.getSubject() + ".");
        } else if (internal) {
            if (t.getAssigneeId() != null && !t.getAssigneeId().equals(me.id())) {
                messenger.tell(users.findAllById(List.of(t.getAssigneeId())), null,
                        me.fullName() + " left an internal note on ticket " + t.getNumber() + ".", link(t));
            }
        } else {
            if (t.getStatus() == Ticket.Status.OPEN) {
                t.moveTo(Ticket.Status.IN_PROGRESS, now);
            }
            messenger.toCompany(t.getCompanyId(), null, me.fullName() + " replied on your ticket " + t.getNumber() + ": " + t.getSubject() + ".", link(t));
        }
        return toDtos(me, List.of(t), true, now).get(0);
    }

    @Transactional
    public TicketDto changeStatus(AuthUser me, UUID id, Ticket.Status target) {
        Ticket t = visible(me, id);
        if (me.isClient()) {
            throw ApiException.forbidden("FORBIDDEN", "Use reopen, close or a comment to answer a ticket.");
        }
        if (target == Ticket.Status.CLOSED && !me.isManagement()) {
            throw ApiException.forbidden("MANAGER_ONLY", "Only the project manager or an administrator can close a ticket.");
        }
        if (t.getStatus() == Ticket.Status.CLOSED) {
            throw ApiException.conflict("TICKET_CLOSED", "This ticket is closed.");
        }
        if (!STAFF_MOVES.get(t.getStatus()).contains(target)) {
            throw ApiException.conflict("INVALID_TRANSITION", "A ticket cannot move from " + t.getStatus() + " to " + target + ".");
        }
        Instant now = Instant.now();
        Ticket.Status from = t.getStatus();
        if (target == Ticket.Status.RESOLVED) {
            t.moveTo(target, now);
            t.resolve(now);
        } else if (target == Ticket.Status.CLOSED) {
            t.close(now);
        } else if (from == Ticket.Status.RESOLVED) {
            t.reopen(now, false);
        } else {
            t.moveTo(target, now);
        }
        messenger.toCompany(t.getCompanyId(), null, "Your ticket " + t.getNumber() + " is now " + label(t.getStatus()) + ".", link(t));
        notifyAssigneeIfOther(t, me.id(), me.fullName() + " changed ticket " + t.getNumber() + " to " + label(t.getStatus()) + ".");
        audit.record(me, "Ticket status changed", t.getNumber() + " · " + from + " to " + t.getStatus());
        return toDtos(me, List.of(t), true, now).get(0);
    }

    @Transactional
    public TicketDto close(AuthUser me, UUID id) {
        Ticket t = visible(me, id);
        if (me.isDeveloper()) {
            throw ApiException.forbidden("FORBIDDEN", "Only the client or the project manager can close a ticket.");
        }
        if (t.getStatus() == Ticket.Status.CLOSED) {
            throw ApiException.conflict("TICKET_CLOSED", "This ticket is already closed.");
        }
        if (me.isManagement() && t.getStatus() != Ticket.Status.RESOLVED) {
            throw ApiException.conflict("INVALID_TRANSITION", "Resolve the ticket before closing it.");
        }
        Instant now = Instant.now();
        t.close(now);
        if (me.isClient()) {
            tellStaffSide(t, me.id(), me.fullName() + " closed ticket " + t.getNumber() + ": " + t.getSubject() + ".");
        } else {
            messenger.toCompany(t.getCompanyId(), null, "Your ticket " + t.getNumber() + " has been closed.", link(t));
        }
        audit.record(me, "Ticket closed", t.getNumber() + " · " + t.getSubject());
        return toDtos(me, List.of(t), true, now).get(0);
    }

    // ---------------------------------------------------------------- management actions

    @Transactional
    public TicketDto assign(AuthUser me, UUID id, AssignInput in) {
        ProjectService.requireManagement(me);
        Ticket t = visible(me, id);
        if (t.getStatus() == Ticket.Status.CLOSED) {
            throw ApiException.conflict("TICKET_CLOSED", "This ticket is closed.");
        }
        UserAccount a = users.findById(in.assigneeId()).orElse(null);
        if (a == null || !a.isActive() || (a.getRole() != Role.DEVELOPER && a.getRole() != Role.PROJECT_MANAGER)) {
            throw ApiException.badRequest("NOT_ASSIGNABLE", "Tickets can only be given to an active developer or project manager.");
        }
        Instant now = Instant.now();
        t.assignTo(a.getId(), now);
        if (!a.getId().equals(me.id())) {
            messenger.tell(List.of(a), null, me.fullName() + " assigned you ticket " + t.getNumber() + ": " + t.getSubject()
                    + " (answer due " + t.getDueAt() + ").", link(t));
        }
        messenger.toCompany(t.getCompanyId(), null, a.getFullName() + " is now looking at your ticket " + t.getNumber() + ".", link(t));
        audit.record(me, "Ticket assigned", t.getNumber() + " · " + a.getFullName());
        return toDtos(me, List.of(t), true, now).get(0);
    }

    @Transactional
    public TicketDto changePriority(AuthUser me, UUID id, Ticket.Priority priority) {
        ProjectService.requireManagement(me);
        Ticket t = visible(me, id);
        if (!t.isActive()) {
            throw ApiException.conflict("INVALID_TRANSITION", "The priority of a finished ticket cannot be changed.");
        }
        Instant now = Instant.now();
        Ticket.Priority old = t.getPriority();
        t.changePriority(priority, now);
        String msg = "Ticket " + t.getNumber() + " is now " + priority.name().toLowerCase() + " priority (answer due in " + t.getSlaHours() + " hours from the start).";
        messenger.toCompany(t.getCompanyId(), null, msg, link(t));
        notifyAssigneeIfOther(t, me.id(), msg);
        audit.record(me, "Ticket priority changed", t.getNumber() + " · " + old + " to " + priority);
        return toDtos(me, List.of(t), true, now).get(0);
    }

    // ---------------------------------------------------------------- helpers

    static String link(Ticket t) {
        return "/tickets/" + t.getId();
    }

    private static String label(Ticket.Status s) {
        return s.name().toLowerCase().replace('_', ' ');
    }

    private static void requireClient(AuthUser me) {
        if (!me.isClient()) {
            throw ApiException.forbidden("CLIENT_ONLY", "Only the client can do this.");
        }
    }

    /** Tells the person handling the ticket, or every manager when nobody is assigned. Never tells the person who did it. */
    private void tellStaffSide(Ticket t, UUID actor, String message) {
        List<UserAccount> to = new ArrayList<>();
        if (t.getAssigneeId() != null) {
            users.findById(t.getAssigneeId()).filter(UserAccount::isActive).ifPresent(to::add);
        } else {
            to.addAll(messenger.management());
        }
        to.removeIf(u -> u.getId().equals(actor));
        messenger.tell(to, null, message, link(t));
    }

    private void notifyAssigneeIfOther(Ticket t, UUID actor, String message) {
        if (t.getAssigneeId() != null && !t.getAssigneeId().equals(actor)) {
            messenger.tell(users.findAllById(List.of(t.getAssigneeId())), null, message, link(t));
        }
    }

    /** The ticket if this person may see it: management all, a client their company's, a developer the ones assigned to them. */
    private Ticket visible(AuthUser me, UUID id) {
        Ticket t = tickets.findById(id).orElseThrow(() -> ApiException.notFound("Ticket not found."));
        boolean ok = switch (me.role()) {
            case ADMIN, PROJECT_MANAGER -> true;
            case CLIENT -> t.getCompanyId().equals(me.companyId());
            case DEVELOPER -> me.id().equals(t.getAssigneeId());
        };
        if (!ok) {
            throw ApiException.notFound("Ticket not found.");
        }
        return t;
    }

    List<TicketDto> toDtos(AuthUser me, List<Ticket> found, boolean withDetail, Instant now) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = found.stream().map(Ticket::getId).toList();
        List<TicketComment> all = withDetail
                ? comments.findByTicketIdOrderByCreatedAtAsc(found.get(0).getId()).stream().filter(c -> !me.isClient() || !c.isInternal()).toList()
                : List.of();
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : comments.countByTicket(ids, !me.isClient())) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        List<UUID> people = new ArrayList<>();
        found.forEach(t -> {
            people.add(t.getCreatedBy());
            people.add(t.getAssigneeId());
        });
        all.forEach(c -> people.add(c.getAuthorId()));
        Map<UUID, UserAccount> accounts = new HashMap<>();
        users.findAllById(people.stream().filter(java.util.Objects::nonNull).distinct().toList()).forEach(u -> accounts.put(u.getId(), u));
        Map<UUID, String> companyNames = names.companies(found.stream().map(Ticket::getCompanyId).toList());
        Map<UUID, String> projectNames = new HashMap<>();
        List<UUID> projectIds = found.stream().map(Ticket::getProjectId).filter(java.util.Objects::nonNull).distinct().toList();
        if (!projectIds.isEmpty()) {
            projects.findAllById(projectIds).forEach(p -> projectNames.put(p.getId(), p.getName()));
        }
        List<TicketDto> out = new ArrayList<>();
        for (Ticket t : found) {
            List<CommentDto> commentDtos = null;
            if (withDetail) {
                commentDtos = all.stream().map(c -> {
                    UserAccount a = accounts.get(c.getAuthorId());
                    return new CommentDto(c.getId(), c.getAuthorId(), a == null ? "Unknown" : a.getFullName(),
                            a == null ? null : a.getRole().name(), c.getBody(), c.isInternal(), c.getCreatedAt());
                }).toList();
            }
            UserAccount creator = accounts.get(t.getCreatedBy());
            UserAccount assignee = accounts.get(t.getAssigneeId());
            out.add(new TicketDto(t.getId(), t.getNumber(), t.getSubject(), t.getPriority(), t.getStatus(), t.getCompanyId(),
                    companyNames.get(t.getCompanyId()), t.getProjectId(), projectNames.get(t.getProjectId()), t.getCreatedBy(),
                    creator == null ? null : creator.getFullName(), t.getAssigneeId(), assignee == null ? null : assignee.getFullName(),
                    t.getSlaHours(), t.getCreatedAt(), t.getDueAt(), t.isBreached(now), t.minutesLeft(now), t.slaState(now),
                    t.getResolvedAt(), t.getResolvedWithinSla(), t.getClosedAt(), t.getReopenCount(),
                    withDetail ? commentDtos.size() : counts.getOrDefault(t.getId(), 0), t.getUpdatedAt(),
                    withDetail ? t.getDescription() : null, commentDtos));
        }
        return out;
    }
}
