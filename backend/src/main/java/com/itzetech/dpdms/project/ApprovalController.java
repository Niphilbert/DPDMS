package com.itzetech.dpdms.project;

import com.itzetech.dpdms.project.ProjectDtos.ApprovalDto;
import com.itzetech.dpdms.project.ProjectDtos.ChangeRequestDto;
import com.itzetech.dpdms.security.AuthUser;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Approvals and change requests on a project. Developers have no access to either. */
@RestController
@RequestMapping("/api/projects/{projectId}")
class ApprovalController {

    record SendApprovalRequest(
            UUID taskId,
            @NotBlank(message = "Enter the name of the item.") @Size(max = 200, message = "Keep the item name under 200 characters.") String item,
            @NotNull(message = "Choose the type of item.") Approval.Type type,
            @NotBlank(message = "Add the file name or a link.") @Size(max = 300, message = "Keep the file or link under 300 characters.") String file,
            @Size(max = 1000, message = "Keep the message under 1000 characters.") String message) {
    }

    record ApproveRequest(@Size(max = 1000, message = "Keep the note under 1000 characters.") String note) {
    }

    record ChangesRequest(
            @NotBlank(message = "Tell the team what needs to change.") @Size(max = 1000, message = "Keep the note under 1000 characters.") String note) {
    }

    record ChangeRequestBody(
            @NotBlank(message = "Describe the extra work.") @Size(max = 200, message = "Keep the title under 200 characters.") String title,
            @NotNull(message = "Enter the extra cost.") @Min(value = 1, message = "The extra cost must be more than zero.") @Max(value = 1_000_000_000_000L, message = "That amount is too large.") Long cost) {
    }

    private final ApprovalService approvals;
    private final ChangeRequestService changes;

    ApprovalController(ApprovalService approvals, ChangeRequestService changes) {
        this.approvals = approvals;
        this.changes = changes;
    }

    @GetMapping("/approvals")
    List<ApprovalDto> approvals(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId) {
        return approvals.list(me, projectId);
    }

    @PostMapping("/approvals")
    @ResponseStatus(HttpStatus.CREATED)
    ApprovalDto send(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId, @Valid @RequestBody SendApprovalRequest r) {
        return approvals.send(me, projectId, r.taskId(), r.item(), r.type(), r.file(), r.message());
    }

    @PostMapping("/approvals/{approvalId}/approve")
    ApprovalDto approve(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId, @PathVariable UUID approvalId,
                        @Valid @RequestBody(required = false) ApproveRequest r) {
        return approvals.approve(me, projectId, approvalId, r == null ? null : r.note());
    }

    @PostMapping("/approvals/{approvalId}/request-changes")
    ApprovalDto requestChanges(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId, @PathVariable UUID approvalId,
                               @Valid @RequestBody ChangesRequest r) {
        return approvals.requestChanges(me, projectId, approvalId, r.note());
    }

    @GetMapping("/change-requests")
    List<ChangeRequestDto> changeRequests(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId) {
        return changes.list(me, projectId);
    }

    @PostMapping("/change-requests")
    @ResponseStatus(HttpStatus.CREATED)
    ChangeRequestDto propose(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId, @Valid @RequestBody ChangeRequestBody r) {
        return changes.propose(me, projectId, r.title(), r.cost());
    }

    @PostMapping("/change-requests/{changeId}/accept")
    ChangeRequestDto accept(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId, @PathVariable UUID changeId) {
        return changes.answer(me, projectId, changeId, true);
    }

    @PostMapping("/change-requests/{changeId}/decline")
    ChangeRequestDto decline(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId, @PathVariable UUID changeId) {
        return changes.answer(me, projectId, changeId, false);
    }
}
