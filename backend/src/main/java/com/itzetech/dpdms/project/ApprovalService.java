package com.itzetech.dpdms.project;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.notification.NotificationService;
import com.itzetech.dpdms.project.ProjectDtos.ApprovalDto;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Things the project manager sends the client to look at, and the client's answer. */
@Service
public class ApprovalService {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final ApprovalRepository approvals;
    private final Names names;
    private final NotificationService notifications;
    private final AuditService audit;

    public ApprovalService(ProjectService projectService, TaskService taskService, ApprovalRepository approvals, Names names,
                           NotificationService notifications, AuditService audit) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.approvals = approvals;
        this.names = names;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ApprovalDto> list(AuthUser me, UUID projectId) {
        Project p = projectService.visible(me, projectId);
        ProjectService.requireNotDeveloper(me);
        return dtos(approvals.findByProjectIdOrderBySentAtDesc(p.getId()));
    }

    @Transactional
    public ApprovalDto send(AuthUser me, UUID projectId, UUID taskId, String item, Approval.Type type, String file, String message) {
        ProjectService.requireManagement(me);
        Project p = projectService.visible(me, projectId);
        if (taskId != null) {
            projectService.task(p, taskId);
        }
        String cleanItem = item.trim();
        int version = approvals.latestVersion(projectId, cleanItem.toLowerCase()) + 1;
        Approval a = approvals.save(new Approval(projectId, taskId, cleanItem, type, file.trim(), version,
                message == null || message.isBlank() ? null : message.trim(), me.id()));
        String label = cleanItem + " (version " + version + ")";
        notifications.notifyCompany(p.getCompanyId(), me.fullName() + " sent you \"" + label + "\" to approve in \"" + p.getName() + "\".",
                ProjectService.link(p));
        projectService.postUpdate(projectId, "Waiting for your approval: " + label);
        audit.record(me, "Approval requested", p.getName() + " · " + label);
        return dtos(List.of(a)).get(0);
    }

    @Transactional
    public ApprovalDto approve(AuthUser me, UUID projectId, UUID approvalId, String note) {
        Project p = projectService.visible(me, projectId);
        ProjectService.requireClient(me);
        Approval a = pending(p, approvalId);
        a.answer(Approval.Status.APPROVED, me.id(), clean(note));
        String label = a.getItem() + " (version " + a.getVersion() + ")";
        tellTeam(me, p, a, me.fullName() + " approved \"" + label + "\" in \"" + p.getName() + "\".");
        projectService.postUpdate(projectId, "Approved: " + label);
        audit.record(me, "Approval given", p.getName() + " · " + label);
        if (a.getTaskId() != null) {
            taskService.markDoneByApproval(me, p, projectService.task(p, a.getTaskId()));
        }
        return dtos(List.of(a)).get(0);
    }

    @Transactional
    public ApprovalDto requestChanges(AuthUser me, UUID projectId, UUID approvalId, String note) {
        Project p = projectService.visible(me, projectId);
        ProjectService.requireClient(me);
        Approval a = pending(p, approvalId);
        a.answer(Approval.Status.CHANGES_REQUESTED, me.id(), note.trim());
        String label = a.getItem() + " (version " + a.getVersion() + ")";
        tellTeam(me, p, a, me.fullName() + " asked for changes to \"" + label + "\" in \"" + p.getName() + "\": " + note.trim());
        projectService.postUpdate(projectId, "Changes requested: " + label);
        audit.record(me, "Changes requested on approval", p.getName() + " · " + label);
        return dtos(List.of(a)).get(0);
    }

    private Approval pending(Project p, UUID approvalId) {
        Approval a = approvals.findByIdAndProjectId(approvalId, p.getId()).orElseThrow(() -> ApiException.notFound("Approval not found."));
        if (a.getStatus() != Approval.Status.PENDING) {
            throw ApiException.conflict("ALREADY_ANSWERED", "This item has already been answered.");
        }
        return a;
    }

    /** The person who sent the item and the project manager both hear about the answer. */
    private void tellTeam(AuthUser client, Project p, Approval a, String message) {
        if (!a.getSentBy().equals(p.getManagerId())) {
            notifications.notifyUser(a.getSentBy(), message, ProjectService.link(p));
        }
        projectService.notifyManager(p, message);
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private List<ApprovalDto> dtos(List<Approval> list) {
        List<UUID> ids = new ArrayList<>();
        list.forEach(a -> {
            ids.add(a.getSentBy());
            ids.add(a.getAnsweredBy());
        });
        Map<UUID, String> people = names.users(ids);
        return list.stream().map(a -> new ApprovalDto(a.getId(), a.getProjectId(), a.getTaskId(), a.getItem(), a.getItemType(),
                a.getFileRef(), a.getVersion(), a.getMessage(), a.getStatus(), a.getAnswerNote(), a.getSentBy(),
                people.get(a.getSentBy()), a.getSentAt(), people.get(a.getAnsweredBy()), a.getAnsweredAt())).toList();
    }
}
