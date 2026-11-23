package com.itzetech.dpdms.project;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.notification.NotificationService;
import com.itzetech.dpdms.project.ProjectDtos.AccessDto;
import com.itzetech.dpdms.project.ProjectDtos.PhaseDto;
import com.itzetech.dpdms.project.ProjectDtos.ProjectDto;
import com.itzetech.dpdms.project.ProjectDtos.TaskDto;
import com.itzetech.dpdms.project.ProjectDtos.UpdateDto;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.user.Role;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import com.itzetech.dpdms.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Projects: who can see what, progress, starting, the updates feed and developer access. */
@Service
public class ProjectService {

    static final String[] PHASES = {"Discovery", "Design", "Build", "Test and launch"};
    private static final String[] STARTER_TITLES = {"Kick-off", "Design", "Development", "Testing and launch"};
    private static final int[] STARTER_HOURS = {8, 20, 60, 16};

    private final ProjectRepository projects;
    private final ProjectTaskRepository tasks;
    private final ProjectAccessRepository access;
    private final ProjectUpdateRepository updates;
    private final ApprovalRepository approvals;
    private final UserAccountRepository users;
    private final Names names;
    private final NotificationService notifications;
    private final AuditService audit;

    public ProjectService(ProjectRepository projects, ProjectTaskRepository tasks, ProjectAccessRepository access,
                          ProjectUpdateRepository updates, ApprovalRepository approvals, UserAccountRepository users,
                          Names names, NotificationService notifications, AuditService audit) {
        this.projects = projects;
        this.tasks = tasks;
        this.access = access;
        this.updates = updates;
        this.approvals = approvals;
        this.users = users;
        this.names = names;
        this.notifications = notifications;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- permissions

    /** The project, if this person may see it. Anything they may not see is reported as not found. */
    public Project visible(AuthUser me, UUID projectId) {
        Project p = projects.findById(projectId).orElseThrow(() -> ApiException.notFound("Project not found."));
        boolean ok = switch (me.role()) {
            case ADMIN, PROJECT_MANAGER -> true;
            case CLIENT -> p.getCompanyId().equals(me.companyId());
            case DEVELOPER -> projects.isVisibleToDeveloper(projectId, me.id());
        };
        if (!ok) {
            throw ApiException.notFound("Project not found.");
        }
        return p;
    }

    public static void requireManagement(AuthUser me) {
        if (!me.isManagement()) {
            throw ApiException.forbidden("FORBIDDEN", "Only the project manager or an administrator can do this.");
        }
    }

    public static void requireClient(AuthUser me) {
        if (!me.isClient()) {
            throw ApiException.forbidden("CLIENT_ONLY", "Only the client can answer this.");
        }
    }

    /** Developers have no business with quotes, money, approvals or change requests. */
    public static void requireNotDeveloper(AuthUser me) {
        if (me.isDeveloper()) {
            throw ApiException.forbidden("FORBIDDEN", "You do not have access to this.");
        }
    }

    public ProjectTask task(Project p, UUID taskId) {
        return tasks.findByIdAndProjectId(taskId, p.getId()).orElseThrow(() -> ApiException.notFound("Task not found."));
    }

    // ---------------------------------------------------------------- creating from an accepted quote

    /** Called when a client accepts a quote: makes the project and its four starter tasks. */
    @Transactional
    public Project createFromQuote(UUID requestId, UUID companyId, String name, LocalDate deadline, long budget, UUID quotedBy) {
        UUID managerId = pickManager(quotedBy);
        LocalDate today = LocalDate.now();
        Project p = projects.save(new Project(requestId, companyId, name, managerId, today, deadline, budget));
        long daysLeft = ChronoUnit.DAYS.between(today, deadline);
        long step = Math.max(7, Math.round(daysLeft / 4.0));
        for (int i = 0; i < STARTER_TITLES.length; i++) {
            tasks.save(new ProjectTask(p.getId(), i, 1, STARTER_TITLES[i], i == 0 ? managerId : null,
                    today.plusDays(7 + i * step), STARTER_HOURS[i]));
        }
        postUpdate(p.getId(), "Your request was accepted and your project has been set up.");
        return p;
    }

    private UUID pickManager(UUID quotedBy) {
        if (quotedBy != null) {
            UserAccount q = users.findById(quotedBy).orElse(null);
            if (q != null && q.isActive() && (q.getRole() == Role.PROJECT_MANAGER || q.getRole() == Role.ADMIN)) {
                return q.getId();
            }
        }
        return users.findByRoleAndStatus(Role.PROJECT_MANAGER, UserStatus.ACTIVE).stream()
                .min(Comparator.comparing(UserAccount::getCreatedAt)).map(UserAccount::getId)
                .or(() -> users.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE).stream()
                        .min(Comparator.comparing(UserAccount::getCreatedAt)).map(UserAccount::getId))
                .orElseThrow(() -> ApiException.conflict("NO_MANAGER", "There is no active project manager to run this project."));
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<ProjectDto> list(AuthUser me) {
        List<Project> found = switch (me.role()) {
            case ADMIN, PROJECT_MANAGER -> projects.findAllByOrderByCreatedAtDesc();
            case CLIENT -> me.companyId() == null ? List.of() : projects.findByCompanyIdOrderByCreatedAtDesc(me.companyId());
            case DEVELOPER -> projects.findVisibleToDeveloper(me.id());
        };
        return toDtos(me, found, false);
    }

    @Transactional(readOnly = true)
    public ProjectDto detail(AuthUser me, UUID id) {
        return toDtos(me, List.of(visible(me, id)), true).get(0);
    }

    List<ProjectDto> toDtos(AuthUser me, List<Project> found, boolean withTasks) {
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ProjectTask>> byProject = tasks.findByProjectIdIn(found.stream().map(Project::getId).toList()).stream()
                .collect(Collectors.groupingBy(ProjectTask::getProjectId));
        List<UUID> personIds = new ArrayList<>();
        found.forEach(p -> personIds.add(p.getManagerId()));
        if (withTasks && !me.isClient()) {
            byProject.values().forEach(l -> l.forEach(t -> {
                personIds.add(t.getAssigneeId());
                personIds.add(t.getSubmittedBy());
            }));
        }
        Map<UUID, String> people = names.users(personIds);
        Map<UUID, String> companyNames = names.companies(found.stream().map(Project::getCompanyId).toList());
        LocalDate today = LocalDate.now();
        List<ProjectDto> out = new ArrayList<>();
        for (Project p : found) {
            List<ProjectTask> ts = byProject.getOrDefault(p.getId(), List.of()).stream()
                    .sorted(Comparator.comparingInt(ProjectTask::getPhase).thenComparingInt(ProjectTask::getSortOrder)
                            .thenComparing(ProjectTask::getCreatedAt))
                    .toList();
            int planned = ts.stream().mapToInt(ProjectTask::getPlannedHours).sum();
            BigDecimal logged = ts.stream().map(ProjectTask::getLoggedHours).reduce(BigDecimal.ZERO, BigDecimal::add);
            int late = (int) ts.stream().filter(t -> t.isLate(today)).count();
            Integer open = me.isDeveloper() ? null : approvals.countByProjectIdAndStatus(p.getId(), Approval.Status.PENDING);
            out.add(new ProjectDto(p.getId(), me.isClient() ? null : p.getRequestId(), p.getName(), p.getStatus(), p.getCompanyId(),
                    companyNames.get(p.getCompanyId()), p.getManagerId(), people.get(p.getManagerId()), p.getStartDate(), p.getDueDate(),
                    me.isDeveloper() ? null : p.getBudget(), percent(ts), phases(ts),
                    me.isClient() ? null : planned, me.isClient() ? null : logged, late, open,
                    withTasks ? ts.stream().map(t -> taskDto(me, t, people, today)).toList() : null, p.getCreatedAt()));
        }
        return out;
    }

    /** Share of the planned hours that is finished, as a whole percentage (0 when there is no work planned). */
    static int percent(List<ProjectTask> ts) {
        int total = ts.stream().mapToInt(ProjectTask::getPlannedHours).sum();
        if (total == 0) {
            return 0;
        }
        int done = ts.stream().filter(t -> t.getStatus() == ProjectTask.Status.DONE).mapToInt(ProjectTask::getPlannedHours).sum();
        return (int) Math.round(100.0 * done / total);
    }

    static List<PhaseDto> phases(List<ProjectTask> ts) {
        List<PhaseDto> out = new ArrayList<>();
        for (int i = 0; i < PHASES.length; i++) {
            int phase = i;
            List<ProjectTask> inPhase = ts.stream().filter(t -> t.getPhase() == phase).toList();
            out.add(new PhaseDto(i, PHASES[i], inPhase.isEmpty() ? null : percent(inPhase), inPhase.size()));
        }
        return out;
    }

    /** A client gets title, phase, status, due date and late flag only: no people, hours or work files. */
    public TaskDto taskDto(AuthUser me, ProjectTask t, Map<UUID, String> people, LocalDate today) {
        boolean late = t.isLate(today);
        if (me.isClient()) {
            return new TaskDto(t.getId(), t.getPhase(), t.getTitle(), t.getStatus(), t.getDueDate(), late,
                    null, null, null, null, null, null, null, null, null, null);
        }
        return new TaskDto(t.getId(), t.getPhase(), t.getTitle(), t.getStatus(), t.getDueDate(), late, t.getPlannedHours(),
                t.getAssigneeId(), people.get(t.getAssigneeId()), t.getLoggedHours(), t.getSubmissionFile(), t.getSubmissionNote(),
                t.getSubmittedAt(), people.get(t.getSubmittedBy()), t.getSentBackNote(), t.getSentBackAt());
    }

    public TaskDto taskDto(AuthUser me, ProjectTask t) {
        List<UUID> ids = new ArrayList<>();
        ids.add(t.getAssigneeId());
        ids.add(t.getSubmittedBy());
        return taskDto(me, t, names.users(ids), LocalDate.now());
    }

    // ---------------------------------------------------------------- starting and finishing

    @Transactional
    public ProjectDto start(AuthUser me, UUID id) {
        requireManagement(me);
        Project p = visible(me, id);
        if (p.getStatus() != Project.Status.PLANNING) {
            throw ApiException.conflict("ALREADY_STARTED", "This project has already started.");
        }
        p.setStatus(Project.Status.IN_PROGRESS);
        postUpdate(p.getId(), "Work has started on your project.");
        notifications.notifyCompany(p.getCompanyId(), "Work has started on your project \"" + p.getName() + "\".", link(p));
        tasks.findByProjectIdOrderByPhaseAscSortOrderAscCreatedAtAsc(p.getId()).stream()
                .map(ProjectTask::getAssigneeId).filter(a -> a != null && !a.equals(me.id())).distinct()
                .forEach(a -> notifications.notifyUser(a, "Project \"" + p.getName() + "\" has started. Your tasks are ready.", link(p)));
        audit.record(me, "Project started", p.getName());
        return detail(me, id);
    }

    /**
     * Keeps the project status in step with its tasks: everything done means launched, and everything handed in
     * means the project is waiting for review. A project that has not been started stays in planning.
     */
    @Transactional
    public void recalculate(AuthUser actor, Project p) {
        if (p.getStatus() == Project.Status.PLANNING) {
            return;
        }
        List<ProjectTask> ts = tasks.findByProjectIdOrderByPhaseAscSortOrderAscCreatedAtAsc(p.getId());
        Project.Status next;
        if (!ts.isEmpty() && ts.stream().allMatch(t -> t.getStatus() == ProjectTask.Status.DONE)) {
            next = Project.Status.LAUNCHED;
        } else if (ts.stream().anyMatch(t -> t.getStatus() == ProjectTask.Status.REVIEW)
                && ts.stream().noneMatch(t -> t.getStatus() == ProjectTask.Status.TODO || t.getStatus() == ProjectTask.Status.DOING)) {
            next = Project.Status.IN_REVIEW;
        } else {
            next = Project.Status.IN_PROGRESS;
        }
        if (next == p.getStatus()) {
            return;
        }
        p.setStatus(next);
        if (next == Project.Status.LAUNCHED) {
            postUpdate(p.getId(), "All work is finished: your project has launched.");
            notifications.notifyCompany(p.getCompanyId(), "Your project \"" + p.getName() + "\" has launched.", link(p));
            notifyManager(p, "Project \"" + p.getName() + "\" is complete and launched.");
            audit.record(actor, "Project launched", p.getName());
        }
    }

    // ---------------------------------------------------------------- developer access (administrator)

    @Transactional(readOnly = true)
    public List<AccessDto> accessList(UUID projectId) {
        projects.findById(projectId).orElseThrow(() -> ApiException.notFound("Project not found."));
        return accessDtos(projectId);
    }

    @Transactional
    public List<AccessDto> grantAccess(AuthUser me, UUID projectId, UUID userId) {
        requireAdmin(me);
        Project p = projects.findById(projectId).orElseThrow(() -> ApiException.notFound("Project not found."));
        UserAccount dev = users.findById(userId).orElseThrow(() -> ApiException.notFound("Person not found."));
        if (dev.getRole() != Role.DEVELOPER || !dev.isActive()) {
            throw ApiException.badRequest("NOT_A_DEVELOPER", "Access can only be given to an active developer.");
        }
        if (access.findByProjectIdAndUserId(projectId, userId).isEmpty()) {
            access.save(new ProjectAccess(projectId, userId, me.id()));
            notifications.notifyUser(userId, "You can now see the project \"" + p.getName() + "\".", link(p));
            audit.record(me, "Project access granted", p.getName() + " · " + dev.getFullName());
        }
        return accessDtos(projectId);
    }

    @Transactional
    public List<AccessDto> revokeAccess(AuthUser me, UUID projectId, UUID userId) {
        requireAdmin(me);
        Project p = projects.findById(projectId).orElseThrow(() -> ApiException.notFound("Project not found."));
        ProjectAccess row = access.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> ApiException.notFound("That developer has not been given access to this project."));
        access.delete(row);
        notifications.notifyUser(userId, "Your access to the project \"" + p.getName() + "\" was removed.", null);
        audit.record(me, "Project access removed", p.getName() + " · " + names.user(userId));
        return accessDtos(projectId);
    }

    private List<AccessDto> accessDtos(UUID projectId) {
        List<ProjectAccess> rows = access.findByProjectIdOrderByGrantedAtAsc(projectId);
        Map<UUID, UserAccount> byId = new HashMap<>();
        users.findAllById(rows.stream().map(ProjectAccess::getUserId).toList()).forEach(u -> byId.put(u.getId(), u));
        return rows.stream().map(r -> {
            UserAccount u = byId.get(r.getUserId());
            return new AccessDto(r.getUserId(), u == null ? "Unknown" : u.getFullName(), u == null ? null : u.getEmail(), r.getGrantedAt());
        }).toList();
    }

    private static void requireAdmin(AuthUser me) {
        if (me.role() != Role.ADMIN) {
            throw ApiException.forbidden("FORBIDDEN", "Only an administrator can do this.");
        }
    }

    // ---------------------------------------------------------------- updates feed

    @Transactional(readOnly = true)
    public List<UpdateDto> feed(AuthUser me, UUID projectId) {
        Project p = visible(me, projectId);
        return updates.findByProjectIdOrderByCreatedAtDesc(p.getId()).stream()
                .map(u -> new UpdateDto(u.getId(), u.getMessage(), u.getCreatedAt())).toList();
    }

    public void postUpdate(UUID projectId, String message) {
        updates.save(new ProjectUpdate(projectId, message));
    }

    // ---------------------------------------------------------------- notification helpers

    public static String link(Project p) {
        return "/projects/" + p.getId();
    }

    /** Tells the person running the project; if they can no longer sign in, tells every project manager. */
    public void notifyManager(Project p, String message) {
        UserAccount m = users.findById(p.getManagerId()).orElse(null);
        if (m != null && m.isActive()) {
            notifications.notifyUser(m.getId(), message, link(p));
        } else {
            notifications.notifyRole(Role.PROJECT_MANAGER, message, link(p));
        }
    }
}
