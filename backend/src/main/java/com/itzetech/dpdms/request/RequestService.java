package com.itzetech.dpdms.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.notification.NotificationService;
import com.itzetech.dpdms.project.Names;
import com.itzetech.dpdms.project.Project;
import com.itzetech.dpdms.project.ProjectService;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Client requests and quotes: sending, quoting, and the client's answer. */
@Service
public class RequestService {

    public record QuoteItemDto(String description, long amount) {
    }

    public record QuoteDto(List<QuoteItemDto> items, long total, int weeks, LocalDate validUntil, Instant sentAt, String quotedByName) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RequestDto(UUID id, UUID companyId, String companyName, String title, String description, String budget,
                             LocalDate deadline, ClientRequest.Status status, String changeNote, QuoteDto quote, UUID projectId,
                             String createdByName, Instant createdAt, Instant updatedAt) {
    }

    public record QuoteLine(String description, long amount) {
    }

    private final ClientRequestRepository requests;
    private final ProjectService projects;
    private final Names names;
    private final NotificationService notifications;
    private final AuditService audit;

    public RequestService(ClientRequestRepository requests, ProjectService projects, Names names,
                          NotificationService notifications, AuditService audit) {
        this.requests = requests;
        this.projects = projects;
        this.names = names;
        this.notifications = notifications;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<RequestDto> list(AuthUser me, ClientRequest.Status status) {
        ProjectService.requireNotDeveloper(me);
        List<ClientRequest> found;
        if (me.isClient()) {
            if (me.companyId() == null) {
                return List.of();
            }
            found = status == null ? requests.findByCompanyIdOrderByCreatedAtDesc(me.companyId())
                    : requests.findByCompanyIdAndStatusOrderByCreatedAtDesc(me.companyId(), status);
        } else {
            found = status == null ? requests.findAllByOrderByCreatedAtDesc() : requests.findByStatusOrderByCreatedAtDesc(status);
        }
        return dtos(found);
    }

    @Transactional(readOnly = true)
    public RequestDto get(AuthUser me, UUID id) {
        return dtos(List.of(load(me, id))).get(0);
    }

    /** A request someone else's company sent is reported as not found; developers have no access to requests at all. */
    private ClientRequest load(AuthUser me, UUID id) {
        ProjectService.requireNotDeveloper(me);
        ClientRequest r = requests.findById(id).orElseThrow(() -> ApiException.notFound("Request not found."));
        if (me.isClient() && !r.getCompanyId().equals(me.companyId())) {
            throw ApiException.notFound("Request not found.");
        }
        return r;
    }

    // ---------------------------------------------------------------- the client sends a request

    @Transactional
    public RequestDto create(AuthUser me, String title, String description, String budget, LocalDate deadline) {
        if (!me.isClient() || me.companyId() == null) {
            throw ApiException.forbidden("CLIENT_ONLY", "Only clients can send a request.");
        }
        if (!deadline.isAfter(LocalDate.now())) {
            throw ApiException.badRequest("DEADLINE_PAST", "Choose a deadline after today.");
        }
        ClientRequest r = requests.save(new ClientRequest(me.companyId(), me.id(), title.trim(), description.trim(),
                budget == null || budget.isBlank() ? null : budget.trim(), deadline));
        String company = names.company(me.companyId());
        String message = "New request from " + company + ": " + r.getTitle();
        notifications.notifyRole(Role.PROJECT_MANAGER, message, link(r));
        notifications.notifyRole(Role.ADMIN, message, link(r));
        audit.record(me, "Request sent", company + " · " + r.getTitle());
        return dtos(List.of(r)).get(0);
    }

    // ---------------------------------------------------------------- the project manager quotes

    @Transactional
    public RequestDto quote(AuthUser me, UUID id, List<QuoteLine> lines, int weeks, LocalDate validUntil) {
        ProjectService.requireManagement(me);
        ClientRequest r = load(me, id);
        if (r.getStatus() == ClientRequest.Status.ACCEPTED || r.getStatus() == ClientRequest.Status.DECLINED) {
            throw ApiException.conflict("INVALID_STATE", "This request is already " + word(r.getStatus()) + ", so it cannot be quoted again.");
        }
        if (validUntil.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("VALID_UNTIL_PAST", "The quote must be valid until today or later.");
        }
        List<QuoteItem> items = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            items.add(new QuoteItem(i + 1, lines.get(i).description().trim(), lines.get(i).amount()));
        }
        boolean revision = r.getStatus() != ClientRequest.Status.NEW;
        r.quote(me.id(), items, weeks, validUntil);
        notifications.notifyCompany(r.getCompanyId(), me.fullName() + (revision ? " sent you a revised quote" : " sent you a quote")
                + " for \"" + r.getTitle() + "\": " + money(r.total()) + ", valid until " + validUntil + ".", link(r));
        audit.record(me, revision ? "Quote revised" : "Quote sent", r.getTitle() + " · " + money(r.total()));
        return dtos(List.of(r)).get(0);
    }

    // ---------------------------------------------------------------- the client answers

    @Transactional
    public RequestDto accept(AuthUser me, UUID id) {
        ClientRequest r = load(me, id);
        ProjectService.requireClient(me);
        requireQuoted(r, "accepted");
        if (r.getQuoteValidUntil() != null && r.getQuoteValidUntil().isBefore(LocalDate.now())) {
            throw ApiException.conflict("QUOTE_EXPIRED", "This quote is no longer valid. Ask the team for a new one.");
        }
        Project p = projects.createFromQuote(r.getId(), r.getCompanyId(), r.getTitle(), r.getDeadline(), r.total(), r.getQuotedBy());
        r.accept(p.getId());
        String company = names.company(r.getCompanyId());
        String message = company + " accepted the quote for \"" + r.getTitle() + "\". The project is ready to plan.";
        notifications.notifyRole(Role.PROJECT_MANAGER, message, ProjectService.link(p));
        notifications.notifyRole(Role.ADMIN, message, ProjectService.link(p));
        audit.record(me, "Quote accepted", company + " · " + r.getTitle() + " · " + money(r.total()));
        return dtos(List.of(r)).get(0);
    }

    @Transactional
    public RequestDto requestChanges(AuthUser me, UUID id, String note) {
        ClientRequest r = load(me, id);
        ProjectService.requireClient(me);
        requireQuoted(r, "changed");
        r.requestChanges(note.trim());
        tellManagement(r, me.fullName() + " asked for changes to the quote for \"" + r.getTitle() + "\": " + note.trim());
        audit.record(me, "Quote changes requested", r.getTitle());
        return dtos(List.of(r)).get(0);
    }

    @Transactional
    public RequestDto decline(AuthUser me, UUID id, String reason) {
        ClientRequest r = load(me, id);
        ProjectService.requireClient(me);
        requireQuoted(r, "declined");
        r.decline(reason == null || reason.isBlank() ? null : reason.trim());
        tellManagement(r, me.fullName() + " declined the quote for \"" + r.getTitle() + "\"."
                + (reason == null || reason.isBlank() ? "" : " Reason: " + reason.trim()));
        audit.record(me, "Quote declined", r.getTitle());
        return dtos(List.of(r)).get(0);
    }

    private void requireQuoted(ClientRequest r, String verb) {
        if (r.getStatus() != ClientRequest.Status.QUOTED) {
            throw ApiException.conflict("INVALID_STATE", r.getStatus() == ClientRequest.Status.NEW
                    ? "There is no quote to be " + verb + " yet."
                    : "This request is " + word(r.getStatus()) + ", so the quote cannot be " + verb + ".");
        }
    }

    /** The person who wrote the quote and every project manager hear about the client's answer. */
    private void tellManagement(ClientRequest r, String message) {
        notifications.notifyRole(Role.PROJECT_MANAGER, message, link(r));
        if (r.getQuotedBy() != null) {
            notifications.notifyUser(r.getQuotedBy(), message, link(r));
        }
    }

    private static String word(ClientRequest.Status s) {
        return switch (s) {
            case NEW -> "new";
            case QUOTED -> "quoted";
            case CHANGES_REQUESTED -> "waiting for a revised quote";
            case ACCEPTED -> "accepted";
            case DECLINED -> "declined";
        };
    }

    private static String link(ClientRequest r) {
        return "/requests/" + r.getId();
    }

    private static String money(long amount) {
        return String.format(Locale.US, "%,d RWF", amount);
    }

    private List<RequestDto> dtos(List<ClientRequest> list) {
        List<UUID> personIds = new ArrayList<>();
        list.forEach(r -> {
            personIds.add(r.getCreatedBy());
            personIds.add(r.getQuotedBy());
        });
        Map<UUID, String> people = names.users(personIds);
        Map<UUID, String> companies = names.companies(list.stream().map(ClientRequest::getCompanyId).toList());
        return list.stream().map(r -> {
            QuoteDto quote = r.getQuoteSentAt() == null ? null : new QuoteDto(
                    r.getItems().stream().map(i -> new QuoteItemDto(i.getDescription(), i.getAmount())).toList(),
                    r.total(), r.getQuoteWeeks(), r.getQuoteValidUntil(), r.getQuoteSentAt(), people.get(r.getQuotedBy()));
            return new RequestDto(r.getId(), r.getCompanyId(), companies.get(r.getCompanyId()), r.getTitle(), r.getDescription(),
                    r.getBudgetText(), r.getDeadline(), r.getStatus(), r.getChangeNote(), quote, r.getProjectId(),
                    people.get(r.getCreatedBy()), r.getCreatedAt(), r.getUpdatedAt());
        }).toList();
    }
}
