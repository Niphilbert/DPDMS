package com.itzetech.dpdms.renewal;

import com.itzetech.dpdms.audit.AuditService;
import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.common.Params;
import com.itzetech.dpdms.notification.Messenger;
import com.itzetech.dpdms.project.Names;
import com.itzetech.dpdms.project.Project;
import com.itzetech.dpdms.project.ProjectRepository;
import com.itzetech.dpdms.project.ProjectService;
import com.itzetech.dpdms.renewal.RenewalDtos.CreateInput;
import com.itzetech.dpdms.renewal.RenewalDtos.HistoryDto;
import com.itzetech.dpdms.renewal.RenewalDtos.RenewInput;
import com.itzetech.dpdms.renewal.RenewalDtos.RenewalDto;
import com.itzetech.dpdms.renewal.RenewalDtos.UpdateInput;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Renewals of domains, hosting, SSL certificates and maintenance plans. */
@Service
public class RenewalService {

    private final RenewalRepository renewals;
    private final RenewalHistoryRepository history;
    private final ProjectRepository projects;
    private final ProjectService projectService;
    private final Names names;
    private final Messenger messenger;
    private final AuditService audit;

    public RenewalService(RenewalRepository renewals, RenewalHistoryRepository history, ProjectRepository projects,
                          ProjectService projectService, Names names, Messenger messenger, AuditService audit) {
        this.renewals = renewals;
        this.history = history;
        this.projects = projects;
        this.projectService = projectService;
        this.names = names;
        this.messenger = messenger;
        this.audit = audit;
    }

    /** The optional filters of the list. */
    public record Filter(String status, String type, UUID companyId, UUID projectId, Integer expiringWithinDays) {
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<RenewalDto> list(AuthUser me, Filter f) {
        Renewal.Status status = Params.enumValue(Renewal.Status.class, f.status(), "status");
        Renewal.Type type = Params.enumValue(Renewal.Type.class, f.type(), "type");
        LocalDate today = LocalDate.now();
        List<Renewal> found = scope(me).stream()
                .filter(r -> status == null || r.status(today) == status)
                .filter(r -> type == null || r.getType() == type)
                .filter(r -> f.companyId() == null || !me.isManagement() || r.getCompanyId().equals(f.companyId()))
                .filter(r -> f.projectId() == null || r.getProjectId().equals(f.projectId()))
                .filter(r -> f.expiringWithinDays() == null || (r.daysLeft(today) >= 0 && r.daysLeft(today) <= f.expiringWithinDays()))
                .sorted(Comparator.comparing(Renewal::getExpiresOn).thenComparing(Renewal::getLabel))
                .toList();
        return toDtos(me, found, false);
    }

    @Transactional(readOnly = true)
    public RenewalDto detail(AuthUser me, UUID id) {
        return toDtos(me, List.of(visible(me, id)), true).get(0);
    }

    @Transactional(readOnly = true)
    public List<RenewalDto> forProject(AuthUser me, UUID projectId) {
        Project p = projectService.visible(me, projectId);
        List<Renewal> found = renewals.findByProjectId(p.getId()).stream()
                .sorted(Comparator.comparing(Renewal::getExpiresOn).thenComparing(Renewal::getLabel)).toList();
        return toDtos(me, found, false);
    }

    // ---------------------------------------------------------------- writing

    @Transactional
    public RenewalDto create(AuthUser me, CreateInput in) {
        ProjectService.requireManagement(me);
        Project p = projectService.visible(me, in.projectId());
        Renewal r = new Renewal(p.getId(), p.getCompanyId(), me.id());
        r.set(in.type(), in.label().trim(), clean(in.provider()), in.expiresOn(), in.price(), clean(in.note()));
        r = renewals.save(r);
        audit.record(me, "Renewal added", describe(r) + " · " + p.getName());
        return toDtos(me, List.of(r), true).get(0);
    }

    @Transactional
    public RenewalDto update(AuthUser me, UUID id, UpdateInput in) {
        ProjectService.requireManagement(me);
        Renewal r = renewals.findById(id).orElseThrow(() -> ApiException.notFound("Renewal not found."));
        r.set(in.type(), in.label().trim(), clean(in.provider()), in.expiresOn(), in.price(), clean(in.note()));
        audit.record(me, "Renewal updated", describe(r) + " · expires " + r.getExpiresOn());
        return toDtos(me, List.of(r), true).get(0);
    }

    @Transactional
    public RenewalDto renew(AuthUser me, UUID id, RenewInput in) {
        ProjectService.requireManagement(me);
        Renewal r = renewals.findById(id).orElseThrow(() -> ApiException.notFound("Renewal not found."));
        if (!in.newExpiresOn().isAfter(r.getExpiresOn())) {
            throw ApiException.badRequest("EXPIRY_NOT_LATER", "The new expiry date must be after " + r.getExpiresOn() + ".");
        }
        long pricePaid = in.price() != null ? in.price() : r.getPrice();
        history.save(new RenewalHistory(r.getId(), LocalDate.now(), r.getExpiresOn(), in.newExpiresOn(), pricePaid, clean(in.note()), me.id()));
        LocalDate previous = r.getExpiresOn();
        r.renewTo(in.newExpiresOn(), in.price());
        messenger.toCompany(r.getCompanyId(), null, "Your " + r.getType().label().toLowerCase() + " " + r.getLabel()
                + " has been renewed. It now runs until " + r.getExpiresOn() + ".", link(r));
        audit.record(me, "Renewal renewed", describe(r) + " · " + previous + " to " + r.getExpiresOn());
        return toDtos(me, List.of(r), true).get(0);
    }

    @Transactional
    public void delete(AuthUser me, UUID id) {
        ProjectService.requireManagement(me);
        Renewal r = renewals.findById(id).orElseThrow(() -> ApiException.notFound("Renewal not found."));
        if (history.existsByRenewalId(id)) {
            throw ApiException.conflict("HAS_HISTORY", "This has been renewed before, so it cannot be deleted.");
        }
        renewals.delete(r);
        audit.record(me, "Renewal deleted", describe(r));
    }

    @Transactional
    public void requestRenewal(AuthUser me, UUID id) {
        ProjectService.requireClient(me);
        Renewal r = renewals.findById(id).filter(x -> x.getCompanyId().equals(me.companyId()))
                .orElseThrow(() -> ApiException.notFound("Renewal not found."));
        messenger.toManagement(null, names.company(r.getCompanyId()) + " asks you to renew the " + r.getType().label().toLowerCase() + " "
                + r.getLabel() + " (expires " + r.getExpiresOn() + ").", link(r));
        audit.record(me, "Renewal requested", describe(r));
    }

    // ---------------------------------------------------------------- helpers

    static String describe(Renewal r) {
        return r.getType().label() + " " + r.getLabel();
    }

    static String link(Renewal r) {
        return "/renewals/" + r.getId();
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** The renewals this person may see. */
    public List<Renewal> scope(AuthUser me) {
        return switch (me.role()) {
            case ADMIN, PROJECT_MANAGER -> renewals.findAll();
            case CLIENT -> me.companyId() == null ? List.of() : renewals.findByCompanyId(me.companyId());
            case DEVELOPER -> {
                List<UUID> ids = projects.findVisibleToDeveloper(me.id()).stream().map(Project::getId).toList();
                yield ids.isEmpty() ? List.of() : renewals.findByProjectIdIn(ids);
            }
        };
    }

    private Renewal visible(AuthUser me, UUID id) {
        Renewal r = renewals.findById(id).orElseThrow(() -> ApiException.notFound("Renewal not found."));
        boolean ok = switch (me.role()) {
            case ADMIN, PROJECT_MANAGER -> true;
            case CLIENT -> r.getCompanyId().equals(me.companyId());
            case DEVELOPER -> projects.isVisibleToDeveloper(r.getProjectId(), me.id());
        };
        if (!ok) {
            throw ApiException.notFound("Renewal not found.");
        }
        return r;
    }

    List<RenewalDto> toDtos(AuthUser me, List<Renewal> found, boolean withHistory) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = found.stream().map(Renewal::getId).toList();
        Map<UUID, List<RenewalHistory>> byRenewal = history.findByRenewalIdInOrderByCreatedAtDesc(ids).stream()
                .collect(Collectors.groupingBy(RenewalHistory::getRenewalId));
        Map<UUID, String> people = withHistory
                ? names.users(byRenewal.values().stream().flatMap(List::stream).map(RenewalHistory::getRenewedBy).toList()) : Map.of();
        Map<UUID, String> companyNames = names.companies(found.stream().map(Renewal::getCompanyId).toList());
        Map<UUID, String> projectNames = new HashMap<>();
        projects.findAllById(found.stream().map(Renewal::getProjectId).distinct().toList()).forEach(p -> projectNames.put(p.getId(), p.getName()));
        LocalDate today = LocalDate.now();
        List<RenewalDto> out = new ArrayList<>();
        for (Renewal r : found) {
            List<RenewalHistory> h = byRenewal.getOrDefault(r.getId(), List.of());
            LocalDate last = h.stream().map(RenewalHistory::getRenewedOn).max(Comparator.naturalOrder()).orElse(null);
            List<HistoryDto> historyDtos = withHistory ? h.stream().map(x -> new HistoryDto(x.getId(), x.getRenewedOn(), x.getPreviousExpiresOn(),
                    x.getNewExpiresOn(), x.getPrice(), x.getNote(), people.get(x.getRenewedBy()))).toList() : null;
            out.add(new RenewalDto(r.getId(), r.getProjectId(), projectNames.get(r.getProjectId()), r.getCompanyId(),
                    companyNames.get(r.getCompanyId()), r.getType(), r.getLabel(), r.getProvider(), r.getExpiresOn(), r.daysLeft(today),
                    r.status(today), me.isDeveloper() ? null : r.getPrice(), me.isClient() ? null : r.getNote(), last, h.size(),
                    historyDtos, r.getCreatedAt(), r.getUpdatedAt()));
        }
        return out;
    }
}
