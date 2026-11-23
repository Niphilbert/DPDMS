package com.itzetech.dpdms.project;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.notification.NotificationService;
import com.itzetech.dpdms.project.ProjectDtos.ChangeRequestDto;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Extra work with its own price. The team proposes it; the client accepts or declines. */
@Service
public class ChangeRequestService {

    private final ProjectService projectService;
    private final ChangeRequestRepository changes;
    private final Names names;
    private final NotificationService notifications;
    private final AuditService audit;

    public ChangeRequestService(ProjectService projectService, ChangeRequestRepository changes, Names names,
                                NotificationService notifications, AuditService audit) {
        this.projectService = projectService;
        this.changes = changes;
        this.names = names;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestDto> list(AuthUser me, UUID projectId) {
        Project p = projectService.visible(me, projectId);
        ProjectService.requireNotDeveloper(me);
        return dtos(changes.findByProjectIdOrderByCreatedAtDesc(p.getId()));
    }

    @Transactional
    public ChangeRequestDto propose(AuthUser me, UUID projectId, String title, long cost) {
        ProjectService.requireManagement(me);
        Project p = projectService.visible(me, projectId);
        ChangeRequest c = changes.save(new ChangeRequest(projectId, title.trim(), cost, me.id()));
        notifications.notifyCompany(p.getCompanyId(), me.fullName() + " sent you a change request for \"" + p.getName() + "\": "
                + c.getTitle() + " (" + money(cost) + ").", ProjectService.link(p));
        projectService.postUpdate(projectId, "Proposed change: " + c.getTitle() + " (" + money(cost) + ")");
        audit.record(me, "Change request sent", p.getName() + " · " + c.getTitle() + " · " + money(cost));
        return dtos(List.of(c)).get(0);
    }

    @Transactional
    public ChangeRequestDto answer(AuthUser me, UUID projectId, UUID changeId, boolean accept) {
        Project p = projectService.visible(me, projectId);
        ProjectService.requireClient(me);
        ChangeRequest c = changes.findByIdAndProjectId(changeId, projectId).orElseThrow(() -> ApiException.notFound("Change request not found."));
        if (c.getStatus() != ChangeRequest.Status.PROPOSED) {
            throw ApiException.conflict("ALREADY_ANSWERED", "This change request has already been answered.");
        }
        c.answer(accept ? ChangeRequest.Status.ACCEPTED : ChangeRequest.Status.DECLINED, me.id());
        if (accept) {
            p.addToBudget(c.getCost());
        }
        String verb = accept ? "accepted" : "declined";
        projectService.notifyManager(p, me.fullName() + " " + verb + " the change request \"" + c.getTitle() + "\" on \"" + p.getName() + "\".");
        if (!c.getProposedBy().equals(p.getManagerId())) {
            notifications.notifyUser(c.getProposedBy(), me.fullName() + " " + verb + " the change request \"" + c.getTitle() + "\".", ProjectService.link(p));
        }
        projectService.postUpdate(projectId, (accept ? "Change accepted: " : "Change declined: ") + c.getTitle());
        audit.record(me, accept ? "Change request accepted" : "Change request declined", p.getName() + " · " + c.getTitle());
        return dtos(List.of(c)).get(0);
    }

    static String money(long amount) {
        return String.format(java.util.Locale.US, "%,d RWF", amount);
    }

    private List<ChangeRequestDto> dtos(List<ChangeRequest> list) {
        List<UUID> ids = new ArrayList<>();
        list.forEach(c -> {
            ids.add(c.getProposedBy());
            ids.add(c.getAnsweredBy());
        });
        Map<UUID, String> people = names.users(ids);
        return list.stream().map(c -> new ChangeRequestDto(c.getId(), c.getProjectId(), c.getTitle(), c.getCost(), c.getStatus(),
                people.get(c.getProposedBy()), c.getCreatedAt(), people.get(c.getAnsweredBy()), c.getAnsweredAt())).toList();
    }
}
