package com.itzetech.dpdms.project;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response shapes. Where a person is not allowed to see a field it is left out of the JSON altogether
 * (for example a developer never receives {@code budget}, and a client never receives {@code assigneeName}).
 */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record PhaseDto(int index, String name, Integer progress, int taskCount) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskDto(UUID id, int phase, String title, ProjectTask.Status status, LocalDate dueDate, boolean late,
                          Integer plannedHours, UUID assigneeId, String assigneeName, BigDecimal loggedHours,
                          String submissionFile, String submissionNote, Instant submittedAt, String submittedByName,
                          String sentBackNote, Instant sentBackAt) {
    }

    /** One task on "my tasks": the task plus the project it belongs to. */
    public record MyTaskDto(TaskDto task, UUID projectId, String projectName, Project.Status projectStatus) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProjectDto(UUID id, UUID requestId, String name, Project.Status status, UUID companyId, String companyName,
                             UUID managerId, String managerName, LocalDate startDate, LocalDate dueDate, Long budget,
                             int progress, List<PhaseDto> phases, Integer plannedHours, BigDecimal loggedHours,
                             Integer lateTasks, Integer openApprovals, List<TaskDto> tasks, Instant createdAt) {
    }

    public record UpdateDto(UUID id, String message, Instant createdAt) {
    }

    public record AccessDto(UUID userId, String fullName, String email, Instant grantedAt) {
    }

    public record TimeLogDto(UUID id, UUID taskId, String taskTitle, UUID userId, String userName, LocalDate date,
                             BigDecimal hours, String note, Instant createdAt) {
    }

    public record TaskTimeDto(UUID taskId, String title, UUID assigneeId, String assigneeName, int plannedHours, BigDecimal loggedHours) {
    }

    public record TimeSummaryDto(int plannedHours, BigDecimal loggedHours, List<TaskTimeDto> tasks, List<TimeLogDto> entries) {
    }

    public record TimeLogResult(TimeLogDto entry, BigDecimal taskLoggedHours, int taskPlannedHours) {
    }

    public record ApprovalDto(UUID id, UUID projectId, UUID taskId, String item, Approval.Type type, String file, int version,
                              String message, Approval.Status status, String answerNote, UUID sentBy, String sentByName,
                              Instant sentAt, String answeredByName, Instant answeredAt) {
    }

    public record ChangeRequestDto(UUID id, UUID projectId, String title, long cost, ChangeRequest.Status status,
                                   String proposedByName, Instant createdAt, String answeredByName, Instant answeredAt) {
    }
}
