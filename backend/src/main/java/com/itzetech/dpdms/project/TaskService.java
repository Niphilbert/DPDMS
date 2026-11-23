package com.itzetech.dpdms.project;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.notification.NotificationService;
import com.itzetech.dpdms.project.ProjectDtos.MyTaskDto;
import com.itzetech.dpdms.project.ProjectDtos.TaskDto;
import com.itzetech.dpdms.project.ProjectDtos.TaskTimeDto;
import com.itzetech.dpdms.project.ProjectDtos.TimeLogDto;
import com.itzetech.dpdms.project.ProjectDtos.TimeLogResult;
import com.itzetech.dpdms.project.ProjectDtos.TimeSummaryDto;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.user.Role;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Everything that happens to a task: planning, assigning, moving, handing in, reviewing and logging time. */
@Service
public class TaskService {

    private final ProjectService projectService;
    private final ProjectRepository projects;
    private final ProjectTaskRepository tasks;
    private final TimeLogRepository logs;
    private final UserAccountRepository users;
    private final Names names;
    private final NotificationService notifications;
    private final AuditService audit;

    public TaskService(ProjectService projectService, ProjectRepository projects, ProjectTaskRepository tasks, TimeLogRepository logs,
                       UserAccountRepository users, Names names, NotificationService notifications, AuditService audit) {
        this.projectService = projectService;
        this.projects = projects;
        this.tasks = tasks;
        this.logs = logs;
        this.users = users;
        this.names = names;
        this.notifications = notifications;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- planning

    @Transactional
    public TaskDto addTask(AuthUser me, UUID projectId, String title, int phase, int plannedHours, LocalDate dueDate, UUID assigneeId) {
        ProjectService.requireManagement(me);
        Project p = projectService.visible(me, projectId);
        if (p.getStatus() == Project.Status.LAUNCHED) {
            throw ApiException.conflict("PROJECT_LAUNCHED", "This project has already launched, so no tasks can be added.");
        }
        requireNotInPast(dueDate);
        UserAccount assignee = assigneeId == null ? null : requireAssignable(assigneeId);
        ProjectTask t = tasks.save(new ProjectTask(projectId, phase, tasks.maxSortOrder(projectId, phase) + 1, title.trim(),
                assignee == null ? null : assignee.getId(), dueDate, plannedHours));
        if (assignee != null && !assignee.getId().equals(me.id())) {
            notifications.notifyUser(assignee.getId(), me.fullName() + " assigned you \"" + t.getTitle() + "\" in \"" + p.getName()
                    + "\", due " + dueDate + ".", ProjectService.link(p));
        }
        audit.record(me, "Task added", p.getName() + " · " + t.getTitle());
        projectService.recalculate(me, p);
        return projectService.taskDto(me, t);
    }

    @Transactional
    public TaskDto assign(AuthUser me, UUID projectId, UUID taskId, UUID assigneeId, LocalDate dueDate, String note) {
        ProjectService.requireManagement(me);
        Project p = projectService.visible(me, projectId);
        ProjectTask t = projectService.task(p, taskId);
        if (t.getStatus() == ProjectTask.Status.DONE) {
            throw ApiException.conflict("TASK_DONE", "This task is already done and cannot be reassigned.");
        }
        requireNotInPast(dueDate);
        UserAccount assignee = requireAssignable(assigneeId);
        UUID previous = t.getAssigneeId();
        t.setAssigneeId(assignee.getId());
        t.setDueDate(dueDate);
        String extra = note == null || note.isBlank() ? "" : " Note: " + note.trim();
        if (!assignee.getId().equals(me.id())) {
            notifications.notifyUser(assignee.getId(), me.fullName() + " assigned you \"" + t.getTitle() + "\" in \"" + p.getName()
                    + "\", due " + dueDate + "." + extra, ProjectService.link(p));
        }
        if (previous != null && !previous.equals(assignee.getId()) && !previous.equals(me.id())) {
            notifications.notifyUser(previous, "\"" + t.getTitle() + "\" in \"" + p.getName() + "\" has been reassigned to "
                    + assignee.getFullName() + ". You no longer need to work on it.", ProjectService.link(p));
        }
        audit.record(me, previous == null ? "Task assigned" : "Task reassigned",
                p.getName() + " · " + t.getTitle() + " → " + assignee.getFullName());
        return projectService.taskDto(me, t);
    }

    // ---------------------------------------------------------------- moving a task

    @Transactional
    public TaskDto changeStatus(AuthUser me, UUID projectId, UUID taskId, ProjectTask.Status to) {
        Project p = projectService.visible(me, projectId);
        requireNotClient(me);
        ProjectTask t = projectService.task(p, taskId);
        requireStarted(p);
        ProjectTask.Status from = t.getStatus();
        if (to == ProjectTask.Status.REVIEW) {
            throw ApiException.conflict("SUBMISSION_REQUIRED", "Submit your work with a file or link and a note to send it for review.");
        }
        if (me.isDeveloper()) {
            requireAssignee(me, t);
            if (to == ProjectTask.Status.DONE) {
                throw ApiException.forbidden("MANAGER_ONLY", "Only the project manager can mark a task as done.");
            }
            boolean allowed = (from == ProjectTask.Status.TODO && to == ProjectTask.Status.DOING)
                    || (from == ProjectTask.Status.DOING && to == ProjectTask.Status.TODO)
                    || (from == ProjectTask.Status.REVIEW && to == ProjectTask.Status.DOING);
            if (!allowed) {
                throw ApiException.conflict("INVALID_TRANSITION", "This task cannot move from " + word(from) + " to " + word(to) + ".");
            }
        } else if (from == to) {
            throw ApiException.conflict("INVALID_TRANSITION", "This task is already " + word(from) + ".");
        }
        t.setStatus(to);
        audit.record(me, "Task moved", p.getName() + " · " + t.getTitle() + " · " + word(from) + " → " + word(to));
        if (to == ProjectTask.Status.DONE) {
            finished(me, p, t);
        }
        projectService.recalculate(me, p);
        return projectService.taskDto(me, t);
    }

    @Transactional
    public TaskDto submit(AuthUser me, UUID projectId, UUID taskId, String file, String note) {
        Project p = projectService.visible(me, projectId);
        requireNotClient(me);
        ProjectTask t = projectService.task(p, taskId);
        requireStarted(p);
        if (me.isDeveloper()) {
            requireAssignee(me, t);
        }
        if (t.getStatus() != ProjectTask.Status.DOING) {
            throw ApiException.conflict("INVALID_TRANSITION", t.getStatus() == ProjectTask.Status.REVIEW
                    ? "This task has already been submitted for review."
                    : "Start the task before submitting it for review.");
        }
        t.submit(me.id(), file.trim(), note == null || note.isBlank() ? null : note.trim());
        if (!me.id().equals(p.getManagerId())) {
            projectService.notifyManager(p, me.fullName() + " submitted \"" + t.getTitle() + "\" for review in \"" + p.getName() + "\".");
        }
        audit.record(me, "Task submitted for review", p.getName() + " · " + t.getTitle());
        projectService.recalculate(me, p);
        return projectService.taskDto(me, t);
    }

    @Transactional
    public TaskDto sendBack(AuthUser me, UUID projectId, UUID taskId, String note) {
        ProjectService.requireManagement(me);
        Project p = projectService.visible(me, projectId);
        ProjectTask t = projectService.task(p, taskId);
        if (t.getStatus() != ProjectTask.Status.REVIEW) {
            throw ApiException.conflict("INVALID_TRANSITION", "Only a task that is waiting for review can be sent back.");
        }
        t.sendBack(me.id(), note.trim());
        if (t.getAssigneeId() != null && !t.getAssigneeId().equals(me.id())) {
            notifications.notifyUser(t.getAssigneeId(), me.fullName() + " sent \"" + t.getTitle() + "\" back with comments: " + note.trim(),
                    ProjectService.link(p));
        }
        audit.record(me, "Task sent back", p.getName() + " · " + t.getTitle());
        projectService.recalculate(me, p);
        return projectService.taskDto(me, t);
    }

    @Transactional
    public TaskDto complete(AuthUser me, UUID projectId, UUID taskId) {
        ProjectService.requireManagement(me);
        Project p = projectService.visible(me, projectId);
        ProjectTask t = projectService.task(p, taskId);
        if (t.getStatus() != ProjectTask.Status.REVIEW) {
            throw ApiException.conflict("INVALID_TRANSITION", "Only a task that is waiting for review can be marked as done.");
        }
        t.setStatus(ProjectTask.Status.DONE);
        audit.record(me, "Task marked done", p.getName() + " · " + t.getTitle());
        finished(me, p, t);
        projectService.recalculate(me, p);
        return projectService.taskDto(me, t);
    }

    /** Used when the client approves an item linked to a task. */
    @Transactional
    public void markDoneByApproval(AuthUser client, Project p, ProjectTask t) {
        if (t.getStatus() == ProjectTask.Status.DONE) {
            return;
        }
        t.setStatus(ProjectTask.Status.DONE);
        audit.record(client, "Task marked done", p.getName() + " · " + t.getTitle() + " (approved by client)");
        finished(client, p, t);
        projectService.recalculate(client, p);
    }

    private void finished(AuthUser actor, Project p, ProjectTask t) {
        projectService.postUpdate(p.getId(), "Finished: " + t.getTitle());
        if (t.getAssigneeId() != null && !t.getAssigneeId().equals(actor.id())) {
            notifications.notifyUser(t.getAssigneeId(), "\"" + t.getTitle() + "\" in \"" + p.getName() + "\" was marked done.", ProjectService.link(p));
        }
    }

    @Transactional(readOnly = true)
    public List<MyTaskDto> mine(AuthUser me) {
        requireNotClient(me);
        List<ProjectTask> mine = tasks.findByAssigneeIdOrderByDueDateAsc(me.id());
        Map<UUID, Project> byId = new HashMap<>();
        projects.findAllById(mine.stream().map(ProjectTask::getProjectId).distinct().toList()).forEach(p -> byId.put(p.getId(), p));
        List<MyTaskDto> out = new ArrayList<>();
        for (ProjectTask t : mine) {
            Project p = byId.get(t.getProjectId());
            out.add(new MyTaskDto(projectService.taskDto(me, t), p.getId(), p.getName(), p.getStatus()));
        }
        return out;
    }

    // ---------------------------------------------------------------- time logs

    @Transactional
    public TimeLogResult logTime(AuthUser me, UUID projectId, UUID taskId, LocalDate date, BigDecimal hours, String note) {
        Project p = projectService.visible(me, projectId);
        requireNotClient(me);
        ProjectTask t = projectService.task(p, taskId);
        requireAssignee(me, t);
        requireStarted(p);
        if (t.getStatus() == ProjectTask.Status.DONE) {
            throw ApiException.conflict("TASK_DONE", "This task is already done, so no more time can be logged on it.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw ApiException.badRequest("DATE_IN_FUTURE", "You cannot log time for a day that has not happened yet.");
        }
        BigDecimal h = hours.setScale(2, RoundingMode.HALF_UP);
        if (h.signum() <= 0) {
            throw ApiException.badRequest("HOURS_INVALID", "Hours must be more than zero.");
        }
        TimeLog log = logs.save(new TimeLog(t.getId(), me.id(), date, h, note == null || note.isBlank() ? null : note.trim()));
        t.addLoggedHours(h);
        audit.record(me, "Time logged", p.getName() + " · " + t.getTitle() + " · " + h + "h");
        return new TimeLogResult(logDto(log, Map.of(t.getId(), t.getTitle()), Map.of(me.id(), me.fullName())), t.getLoggedHours(), t.getPlannedHours());
    }

    @Transactional(readOnly = true)
    public List<TimeLogDto> taskLogs(AuthUser me, UUID projectId, UUID taskId) {
        Project p = projectService.visible(me, projectId);
        requireNotClient(me);
        ProjectTask t = projectService.task(p, taskId);
        if (me.isDeveloper()) {
            requireAssignee(me, t);
        }
        List<TimeLog> found = logs.findByTaskIdOrderByWorkDateDescCreatedAtDesc(taskId);
        Map<UUID, String> people = names.users(found.stream().map(TimeLog::getUserId).toList());
        return found.stream().map(l -> logDto(l, Map.of(t.getId(), t.getTitle()), people)).toList();
    }

    /** Planned against logged hours for each task. A developer only sees their own tasks. */
    @Transactional(readOnly = true)
    public TimeSummaryDto summary(AuthUser me, UUID projectId) {
        Project p = projectService.visible(me, projectId);
        requireNotClient(me);
        List<ProjectTask> ts = tasks.findByProjectIdOrderByPhaseAscSortOrderAscCreatedAtAsc(projectId).stream()
                .filter(t -> !me.isDeveloper() || me.id().equals(t.getAssigneeId())).toList();
        Map<UUID, String> titles = new HashMap<>();
        ts.forEach(t -> titles.put(t.getId(), t.getTitle()));
        List<TimeLog> entries = ts.isEmpty() ? List.of() : logs.findByTaskIdInOrderByWorkDateDescCreatedAtDesc(titles.keySet());
        List<UUID> personIds = new ArrayList<>(entries.stream().map(TimeLog::getUserId).toList());
        ts.forEach(t -> personIds.add(t.getAssigneeId()));
        Map<UUID, String> people = names.users(personIds);
        List<TaskTimeDto> perTask = ts.stream().map(t -> new TaskTimeDto(t.getId(), t.getTitle(), t.getAssigneeId(),
                people.get(t.getAssigneeId()), t.getPlannedHours(), t.getLoggedHours())).toList();
        int planned = ts.stream().mapToInt(ProjectTask::getPlannedHours).sum();
        BigDecimal logged = ts.stream().map(ProjectTask::getLoggedHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TimeSummaryDto(planned, logged, perTask, entries.stream().map(l -> logDto(l, titles, people)).toList());
    }

    private static TimeLogDto logDto(TimeLog l, Map<UUID, String> titles, Map<UUID, String> people) {
        return new TimeLogDto(l.getId(), l.getTaskId(), titles.get(l.getTaskId()), l.getUserId(), people.get(l.getUserId()),
                l.getWorkDate(), l.getHours(), l.getNote(), l.getCreatedAt());
    }

    // ---------------------------------------------------------------- checks

    private static void requireNotClient(AuthUser me) {
        if (me.isClient()) {
            throw ApiException.forbidden("FORBIDDEN", "You do not have access to this.");
        }
    }

    private static void requireAssignee(AuthUser me, ProjectTask t) {
        if (!me.id().equals(t.getAssigneeId())) {
            throw ApiException.forbidden("NOT_YOUR_TASK", "This task is not assigned to you.");
        }
    }

    private static void requireStarted(Project p) {
        if (p.getStatus() == Project.Status.PLANNING) {
            throw ApiException.conflict("PROJECT_NOT_STARTED", "The project has not been started yet.");
        }
    }

    private static void requireNotInPast(LocalDate due) {
        if (due.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("DUE_DATE_PAST", "Choose a due date from today onwards.");
        }
    }

    private UserAccount requireAssignable(UUID userId) {
        UserAccount u = users.findById(userId).orElseThrow(() -> ApiException.badRequest("NOT_ASSIGNABLE", "That person does not exist."));
        if (!u.isActive() || (u.getRole() != Role.DEVELOPER && u.getRole() != Role.PROJECT_MANAGER)) {
            throw ApiException.badRequest("NOT_ASSIGNABLE", "Tasks can only be given to an active developer or project manager.");
        }
        return u;
    }

    private static String word(ProjectTask.Status s) {
        return switch (s) {
            case TODO -> "to do";
            case DOING -> "in progress";
            case REVIEW -> "in review";
            case DONE -> "done";
        };
    }
}
