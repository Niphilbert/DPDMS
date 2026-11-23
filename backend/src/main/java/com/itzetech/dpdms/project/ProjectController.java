package com.itzetech.dpdms.project;

import com.itzetech.dpdms.project.ProjectDtos.MyTaskDto;
import com.itzetech.dpdms.project.ProjectDtos.ProjectDto;
import com.itzetech.dpdms.project.ProjectDtos.TaskDto;
import com.itzetech.dpdms.project.ProjectDtos.TimeLogDto;
import com.itzetech.dpdms.project.ProjectDtos.TimeLogResult;
import com.itzetech.dpdms.project.ProjectDtos.TimeSummaryDto;
import com.itzetech.dpdms.project.ProjectDtos.UpdateDto;
import com.itzetech.dpdms.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Projects, their tasks, time logs and the updates feed. Who may do what is decided in the services. */
@RestController
class ProjectController {

    record CreateTaskRequest(
            @NotBlank(message = "Enter the task name.") @Size(max = 200, message = "Keep the task name under 200 characters.") String title,
            @NotNull(message = "Choose a phase (0 to 3).") @Min(value = 0, message = "Choose a phase from 0 to 3.") @Max(value = 3, message = "Choose a phase from 0 to 3.") Integer phase,
            @NotNull(message = "Enter the planned hours.") @Min(value = 1, message = "Planned hours must be at least 1.") @Max(value = 2000, message = "Planned hours cannot be more than 2000.") Integer plannedHours,
            @NotNull(message = "Choose a due date.") LocalDate dueDate,
            UUID assigneeId) {
    }

    record AssignRequest(
            @NotNull(message = "Choose who will do this task.") UUID assigneeId,
            @NotNull(message = "Choose a due date.") LocalDate dueDate,
            @Size(max = 500, message = "Keep the note under 500 characters.") String note) {
    }

    record StatusRequest(@NotNull(message = "Choose a status.") ProjectTask.Status status) {
    }

    record SubmitRequest(
            @NotBlank(message = "Add the file name or a link to your work.") @Size(max = 300, message = "Keep the file or link under 300 characters.") String file,
            @Size(max = 1000, message = "Keep the note under 1000 characters.") String note) {
    }

    record SendBackRequest(
            @NotBlank(message = "Tell the developer what needs to change.") @Size(max = 1000, message = "Keep the comments under 1000 characters.") String note) {
    }

    record LogTimeRequest(
            @NotNull(message = "Choose the day you worked.") LocalDate date,
            @NotNull(message = "Enter the hours.") @DecimalMin(value = "0.01", message = "Hours must be more than zero.") @DecimalMax(value = "24", message = "A day has only 24 hours.") BigDecimal hours,
            @Size(max = 500, message = "Keep the note under 500 characters.") String note) {
    }

    private final ProjectService projects;
    private final TaskService tasks;

    ProjectController(ProjectService projects, TaskService tasks) {
        this.projects = projects;
        this.tasks = tasks;
    }

    @GetMapping("/api/projects")
    List<ProjectDto> list(@AuthenticationPrincipal AuthUser me) {
        return projects.list(me);
    }

    @GetMapping("/api/projects/{id}")
    ProjectDto get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return projects.detail(me, id);
    }

    @PostMapping("/api/projects/{id}/start")
    ProjectDto start(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return projects.start(me, id);
    }

    @GetMapping("/api/projects/{id}/updates")
    List<UpdateDto> updates(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return projects.feed(me, id);
    }

    @PostMapping("/api/projects/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    TaskDto addTask(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody CreateTaskRequest r) {
        return tasks.addTask(me, id, r.title(), r.phase(), r.plannedHours(), r.dueDate(), r.assigneeId());
    }

    @PostMapping("/api/projects/{id}/tasks/{taskId}/assign")
    TaskDto assign(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @PathVariable UUID taskId, @Valid @RequestBody AssignRequest r) {
        return tasks.assign(me, id, taskId, r.assigneeId(), r.dueDate(), r.note());
    }

    @PostMapping("/api/projects/{id}/tasks/{taskId}/status")
    TaskDto status(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @PathVariable UUID taskId, @Valid @RequestBody StatusRequest r) {
        return tasks.changeStatus(me, id, taskId, r.status());
    }

    @PostMapping("/api/projects/{id}/tasks/{taskId}/submit")
    TaskDto submit(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @PathVariable UUID taskId, @Valid @RequestBody SubmitRequest r) {
        return tasks.submit(me, id, taskId, r.file(), r.note());
    }

    @PostMapping("/api/projects/{id}/tasks/{taskId}/send-back")
    TaskDto sendBack(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @PathVariable UUID taskId, @Valid @RequestBody SendBackRequest r) {
        return tasks.sendBack(me, id, taskId, r.note());
    }

    @PostMapping("/api/projects/{id}/tasks/{taskId}/complete")
    TaskDto complete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @PathVariable UUID taskId) {
        return tasks.complete(me, id, taskId);
    }

    @GetMapping("/api/tasks/mine")
    List<MyTaskDto> mine(@AuthenticationPrincipal AuthUser me) {
        return tasks.mine(me);
    }

    @PostMapping("/api/projects/{id}/tasks/{taskId}/time")
    @ResponseStatus(HttpStatus.CREATED)
    TimeLogResult logTime(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @PathVariable UUID taskId, @Valid @RequestBody LogTimeRequest r) {
        return tasks.logTime(me, id, taskId, r.date(), r.hours(), r.note());
    }

    @GetMapping("/api/projects/{id}/tasks/{taskId}/time")
    List<TimeLogDto> taskTime(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @PathVariable UUID taskId) {
        return tasks.taskLogs(me, id, taskId);
    }

    @GetMapping("/api/projects/{id}/time")
    TimeSummaryDto time(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return tasks.summary(me, id);
    }
}
