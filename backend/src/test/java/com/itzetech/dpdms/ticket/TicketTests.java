package com.itzetech.dpdms.ticket;

import com.itzetech.dpdms.support.MoneyCareTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketTests extends MoneyCareTestBase {

    private ResultActions setStatus(String token, String ticketId, String status) throws Exception {
        return postJson("/api/tickets/" + ticketId + "/status", json("status", status), token);
    }

    private ResultActions slaCheck(String json) throws Exception {
        return postJson("/api/admin/tickets/run-sla-check", json, adminToken());
    }

    private Instant instantOf(String token, String ticketId, String field) throws Exception {
        return Instant.parse(read(getAs("/api/tickets/" + ticketId, token).andExpect(status().isOk()), "$." + field));
    }

    private List<String> alertKinds(ResultActions run, String ticketId) throws Exception {
        String body = run.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.alerts[?(@.ticketId=='" + ticketId + "')].kind");
    }

    private long countFor(String token, String ticketNumber, String text) throws Exception {
        return notificationMessages(token).stream().filter(m -> m.contains(ticketNumber) && m.contains(text)).count();
    }

    private void moveDeadline(String ticketId, Instant start, Instant due) {
        jdbc.update("update ticket set sla_started_at = ?, due_at = ? where id = ?", Timestamp.from(start), Timestamp.from(due), UUID.fromString(ticketId));
    }

    @Test
    void theDeadlineIsFixedWhenTheTicketIsRaisedAndDependsOnThePriority() throws Exception {
        Scenario s = scenario();
        int[] hours = {4, 8, 24, 72};
        String[] priorities = {"URGENT", "HIGH", "MEDIUM", "LOW"};
        int previousNumber = -1;
        for (int i = 0; i < 4; i++) {
            ResultActions r = postJson("/api/tickets", "{\"subject\":\"Slow site\",\"description\":\"Pages take ages.\",\"priority\":\"" + priorities[i] + "\",\"projectId\":\"" + s.projectId() + "\"}", s.client())
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN")).andExpect(jsonPath("$.slaHours").value(hours[i]))
                    .andExpect(jsonPath("$.number").value(matchesPattern("TCK-\\d{4,}"))).andExpect(jsonPath("$.breached").value(false))
                    .andExpect(jsonPath("$.slaState").value("ON_TRACK")).andExpect(jsonPath("$.minutesLeft").value(lessThanOrEqualTo(hours[i] * 60)))
                    .andExpect(jsonPath("$.minutesLeft").value(greaterThanOrEqualTo(hours[i] * 60 - 2))).andExpect(jsonPath("$.projectName").value("Booking website"))
                    .andExpect(jsonPath("$.assigneeId").value((Object) null)).andExpect(jsonPath("$.reopenCount").value(0)).andExpect(jsonPath("$.commentCount").value(0))
                    .andExpect(jsonPath("$.description").value("Pages take ages.")).andExpect(jsonPath("$.comments", hasSize(0)))
                    .andExpect(jsonPath("$.createdByName").value("Client " + read(getAs("/api/auth/me", s.client()), "$.companyName")));
            String body = r.andReturn().getResponse().getContentAsString();
            Duration window = Duration.between(Instant.parse(JsonPath.read(body, "$.createdAt")), Instant.parse(JsonPath.read(body, "$.dueAt")));
            assertThat(window).isEqualTo(Duration.ofHours(hours[i]));
            int number = Integer.parseInt(((String) JsonPath.read(body, "$.number")).substring(4));
            assertThat(number).isGreaterThan(previousNumber);
            previousNumber = number;
        }
        // raising it is checked
        postJson("/api/tickets", "{}", s.client()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.subject").exists())
                .andExpect(jsonPath("$.fields.description").exists()).andExpect(jsonPath("$.fields.priority").exists());
        postJson("/api/tickets", "{\"subject\":\"x\",\"description\":\"y\",\"priority\":\"SOON\"}", s.client()).andExpect(status().isBadRequest());
        Scenario other = scenario();
        postJson("/api/tickets", "{\"subject\":\"x\",\"description\":\"y\",\"priority\":\"LOW\",\"projectId\":\"" + other.projectId() + "\"}", s.client())
                .andExpect(status().isNotFound());
        // only clients raise tickets; the project is optional
        postJson("/api/tickets", "{\"subject\":\"x\",\"description\":\"y\",\"priority\":\"LOW\"}", s.pm()).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CLIENT_ONLY"));
        postJson("/api/tickets", "{\"subject\":\"x\",\"description\":\"y\",\"priority\":\"LOW\"}", s.dev()).andExpect(status().isForbidden());
        postJson("/api/tickets", "{\"subject\":\"x\",\"description\":\"y\",\"priority\":\"LOW\"}", s.client()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value((Object) null));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Ticket opened")));
    }

    @Test
    void newTicketsReachManagementAndVisibilityFollowsTheRole() throws Exception {
        Scenario s = scenario();
        Scenario other = scenario();
        long adminBefore = countNotifications(adminToken(), "New urgent support ticket");
        long pmBefore = countNotifications(s.pm(), "New urgent support ticket");
        String id = newTicket(s.client(), "URGENT", null);
        assertThat(countNotifications(adminToken(), "New urgent support ticket")).isEqualTo(adminBefore + 1);
        assertThat(countNotifications(s.pm(), "New urgent support ticket")).isEqualTo(pmBefore + 1);

        getAs("/api/tickets/" + id, s.client()).andExpect(status().isOk());
        getAs("/api/tickets/" + id, other.client()).andExpect(status().isNotFound());
        getAs("/api/tickets/" + id, s.dev()).andExpect(status().isNotFound());
        getAs("/api/tickets/" + id, adminToken()).andExpect(status().isOk());
        getAs("/api/tickets", s.client()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].comments").doesNotExist());
        getAs("/api/tickets", other.client()).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/tickets", s.dev()).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/tickets?unassigned=true", s.pm()).andExpect(jsonPath("$[*].id", hasItem(id)));
        getAs("/api/tickets?status=NOPE", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        getAs("/api/tickets", null).andExpect(status().isUnauthorized());

        // assigning: management only, and only to a developer or project manager
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.devId()), s.client()).andExpect(status().isForbidden());
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.devId()), s.dev()).andExpect(status().isForbidden());
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.clientId()), s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_ASSIGNABLE"));
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", meId(adminToken())), s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_ASSIGNABLE"));
        postJson("/api/tickets/" + id + "/assign", "{}", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.assigneeId").exists());
        long devBefore = countNotifications(s.dev(), "assigned you ticket");
        long clientBefore = countNotifications(s.client(), "is now looking at your ticket");
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.devId()), s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(s.devId())).andExpect(jsonPath("$.assigneeName").value("Test DEVELOPER"));
        assertThat(countNotifications(s.dev(), "assigned you ticket")).isEqualTo(devBefore + 1);
        assertThat(countNotifications(s.client(), "is now looking at your ticket")).isEqualTo(clientBefore + 1);

        // now the developer sees it, the other developer still does not, and the filters find it
        getAs("/api/tickets/" + id, s.dev()).andExpect(status().isOk());
        getAs("/api/tickets/" + id, s.dev2()).andExpect(status().isNotFound());
        getAs("/api/tickets", s.dev()).andExpect(jsonPath("$", hasSize(1)));
        getAs("/api/tickets?assigneeId=" + s.devId(), s.pm()).andExpect(jsonPath("$", hasSize(1)));
        getAs("/api/tickets?unassigned=true", s.pm()).andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.not(hasItem(id))));
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.pmId()), s.pm()).andExpect(status().isOk());
        getAs("/api/tickets/" + id, s.dev()).andExpect(status().isNotFound());
        getAs("/api/tickets?mine=true", s.pm()).andExpect(jsonPath("$", hasSize(1)));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Ticket assigned")));
    }

    @Test
    void aTicketMovesThroughItsStatusesWithTheRightPeopleDoingIt() throws Exception {
        Scenario s = scenario();
        String id = newTicket(s.client(), "HIGH", null);
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.devId()), s.pm()).andExpect(status().isOk());

        // the client cannot set a status; an unrelated developer cannot see the ticket
        setStatus(s.client(), id, "RESOLVED").andExpect(status().isForbidden());
        setStatus(s.dev2(), id, "IN_PROGRESS").andExpect(status().isNotFound());
        setStatus(s.dev(), id, "OPEN").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        setStatus(s.dev(), id, "NOPE").andExpect(status().isBadRequest());
        long before = countNotifications(s.client(), "is now in progress");
        setStatus(s.dev(), id, "IN_PROGRESS").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        assertThat(countNotifications(s.client(), "is now in progress")).isEqualTo(before + 1);
        setStatus(s.dev(), id, "WAITING_CLIENT").andExpect(status().isOk());

        // the client answers: back to work by itself
        postJson("/api/tickets/" + id + "/comments", commentBody("It is the checkout page.", false), s.client()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS")).andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].authorRole").value("CLIENT")).andExpect(jsonPath("$.comments[0].internal").value(false));
        assertThat(countNotifications(s.dev(), "replied on ticket")).isEqualTo(1);

        // resolving records when, and whether it was in time
        setStatus(s.dev(), id, "CLOSED").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("MANAGER_ONLY"));
        setStatus(s.dev(), id, "RESOLVED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty()).andExpect(jsonPath("$.resolvedWithinSla").value(true))
                .andExpect(jsonPath("$.minutesLeft").value((Object) null)).andExpect(jsonPath("$.slaState").value("MET")).andExpect(jsonPath("$.breached").value(false));
        setStatus(s.dev(), id, "WAITING_CLIENT").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        postJson("/api/tickets/" + id + "/priority", json("priority", "LOW"), s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        // the client may reopen once: the resolution is forgotten and the clock starts again
        postJson("/api/tickets/" + id + "/reopen", "{}", s.dev()).andExpect(status().isForbidden());
        postJson("/api/tickets/" + id + "/reopen", "{}", s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.reopenCount").value(1)).andExpect(jsonPath("$.resolvedAt").value((Object) null))
                .andExpect(jsonPath("$.resolvedWithinSla").value((Object) null)).andExpect(jsonPath("$.minutesLeft").value(greaterThanOrEqualTo(8 * 60 - 2)));
        postJson("/api/tickets/" + id + "/reopen", "{}", s.client()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        setStatus(s.dev(), id, "RESOLVED").andExpect(status().isOk());
        postJson("/api/tickets/" + id + "/reopen", "{}", s.client()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REOPEN_LIMIT"));

        // closing: the client may; management only after resolving; nothing changes afterwards
        postJson("/api/tickets/" + id + "/close", "{}", s.dev()).andExpect(status().isForbidden());
        postJson("/api/tickets/" + id + "/close", "{}", s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED")).andExpect(jsonPath("$.closedAt").isNotEmpty());
        postJson("/api/tickets/" + id + "/close", "{}", s.client()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TICKET_CLOSED"));
        postJson("/api/tickets/" + id + "/comments", commentBody("hello", false), s.client()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TICKET_CLOSED"));
        setStatus(s.pm(), id, "IN_PROGRESS").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TICKET_CLOSED"));
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.pmId()), s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TICKET_CLOSED"));
        getAs("/api/tickets/" + id, s.client()).andExpect(jsonPath("$.slaState").value("MET"));

        // management: cannot close what is not resolved yet, can close what is; staff can reopen a resolution
        String second = newTicket(s.client(), "LOW", null);
        postJson("/api/tickets/" + second + "/close", "{}", s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        setStatus(s.pm(), second, "RESOLVED").andExpect(status().isOk());
        setStatus(s.pm(), second, "IN_PROGRESS").andExpect(status().isOk()).andExpect(jsonPath("$.reopenCount").value(0)).andExpect(jsonPath("$.resolvedAt").value((Object) null));
        setStatus(s.pm(), second, "RESOLVED").andExpect(status().isOk());
        setStatus(s.pm(), second, "CLOSED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Ticket status changed")))
                .andExpect(jsonPath("$[*].action", hasItem("Ticket reopened"))).andExpect(jsonPath("$[*].action", hasItem("Ticket closed")));
    }

    @Test
    void aStaffReplyOnAnOpenTicketStartsWorkAndInternalNotesNeverReachTheClient() throws Exception {
        Scenario s = scenario();
        String id = newTicket(s.client(), "MEDIUM", null);
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.devId()), s.pm()).andExpect(status().isOk());

        postJson("/api/tickets/" + id + "/comments", "{\"body\":\"secret\",\"internal\":true}", s.client()).andExpect(status().isForbidden());
        postJson("/api/tickets/" + id + "/comments", "{\"body\":\"\"}", s.client()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.body").exists());
        postJson("/api/tickets/" + id + "/comments", commentBody("hi", false), s.dev2()).andExpect(status().isNotFound());

        long notesBefore = countNotifications(s.dev(), "internal note");
        postJson("/api/tickets/" + id + "/comments", commentBody("Customer database looks corrupted, do not tell them yet", true), s.pm()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
        assertThat(countNotifications(s.dev(), "internal note")).isEqualTo(notesBefore + 1);
        long clientBefore = countNotifications(s.client(), "replied on your ticket");
        postJson("/api/tickets/" + id + "/comments", commentBody("Working on a quick fix", true), s.dev()).andExpect(status().isCreated());
        assertThat(countNotifications(s.client(), "replied on your ticket")).isEqualTo(clientBefore);
        postJson("/api/tickets/" + id + "/comments", commentBody("We are looking into it now.", false), s.dev()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS")).andExpect(jsonPath("$.comments", hasSize(3)));
        assertThat(countNotifications(s.client(), "replied on your ticket")).isEqualTo(clientBefore + 1);

        // staff see all three, the client only the public one, and the count agrees
        for (String staff : new String[]{s.pm(), s.dev(), adminToken()}) {
            getAs("/api/tickets/" + id, staff).andExpect(jsonPath("$.comments", hasSize(3))).andExpect(jsonPath("$.commentCount").value(3));
        }
        String clientView = getAs("/api/tickets/" + id, s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].body").value("We are looking into it now.")).andExpect(jsonPath("$.commentCount").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(clientView).doesNotContain("database").doesNotContain("quick fix").doesNotContain("\"internal\":true");
        getAs("/api/tickets", s.client()).andExpect(jsonPath("$[0].commentCount").value(1));
        getAs("/api/tickets?companyId=" + companyIdOf(s.pm(), s.projectId()), s.pm()).andExpect(jsonPath("$[0].commentCount").value(3));
        for (String note : notificationMessages(s.client())) {
            assertThat(note).doesNotContain("database").doesNotContain("quick fix");
        }
    }

    @Test
    void priorityChangesRecalculateTheDeadline() throws Exception {
        Scenario s = scenario();
        String id = newTicket(s.client(), "LOW", null);
        Instant created = instantOf(s.client(), id, "createdAt");
        postJson("/api/tickets/" + id + "/priority", json("priority", "URGENT"), s.client()).andExpect(status().isForbidden());
        postJson("/api/tickets/" + id + "/priority", json("priority", "URGENT"), s.dev()).andExpect(status().isForbidden());
        postJson("/api/tickets/" + id + "/priority", "{}", s.pm()).andExpect(status().isBadRequest());
        postJson("/api/tickets/" + id + "/priority", json("priority", "URGENT"), s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT")).andExpect(jsonPath("$.slaHours").value(4));
        assertThat(instantOf(s.pm(), id, "dueAt")).isEqualTo(created.plus(Duration.ofHours(4)));
        assertThat(countNotifications(s.client(), "is now urgent priority")).isEqualTo(1);
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Ticket priority changed")));
    }

    @Test
    void aMissedDeadlineShowsAsBreachedAndResolvingLateIsRecorded() throws Exception {
        Scenario s = scenario();
        String late = newTicket(s.client(), "URGENT", null);
        Instant now = Instant.now();
        moveDeadline(late, now.minus(Duration.ofHours(6)), now.minus(Duration.ofHours(2)));
        getAs("/api/tickets/" + late, s.client()).andExpect(jsonPath("$.breached").value(true)).andExpect(jsonPath("$.slaState").value("BREACHED"))
                .andExpect(jsonPath("$.minutesLeft").value(lessThanOrEqualTo(-119)));
        getAs("/api/tickets?breached=true", s.pm()).andExpect(jsonPath("$[*].id", hasItem(late)));
        String fine = newTicket(s.client(), "LOW", null);
        getAs("/api/tickets?breached=true", s.pm()).andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.not(hasItem(fine))));

        // the most urgent deadline is listed first among the open tickets
        getAs("/api/tickets", s.client()).andExpect(jsonPath("$[0].id").value(late)).andExpect(jsonPath("$[1].id").value(fine));
        setStatus(s.pm(), late, "RESOLVED").andExpect(status().isOk()).andExpect(jsonPath("$.resolvedWithinSla").value(false))
                .andExpect(jsonPath("$.slaState").value("MISSED")).andExpect(jsonPath("$.breached").value(false));
        // finished tickets go below the open ones
        getAs("/api/tickets", s.client()).andExpect(jsonPath("$[0].id").value(fine)).andExpect(jsonPath("$[1].id").value(late));
    }

    @Test
    void theDeadlineCheckAlertsOncePerKindAndOnlyForOpenTickets() throws Exception {
        Scenario s = scenario();
        String id = newTicket(s.client(), "URGENT", null);
        postJson("/api/tickets/" + id + "/assign", json("assigneeId", s.devId()), s.pm()).andExpect(status().isOk());
        Instant created = instantOf(s.pm(), id, "createdAt");

        // only the administrator may run it
        postJson("/api/admin/tickets/run-sla-check", "{}", s.pm()).andExpect(status().isForbidden());
        postJson("/api/admin/tickets/run-sla-check", "{}", s.dev()).andExpect(status().isForbidden());
        postJson("/api/admin/tickets/run-sla-check", "{}", s.client()).andExpect(status().isForbidden());
        slaCheck("{\"now\":\"yesterday\"}").andExpect(status().isBadRequest());

        // 2 hours of 4 used: nothing yet
        assertThat(alertKinds(slaCheck("{\"now\":\"" + created.plus(Duration.ofHours(2)) + "\"}"), id)).isEmpty();
        // 3.5 hours used: at risk, once
        String atRisk = "{\"now\":\"" + created.plus(Duration.ofMinutes(210)) + "\"}";
        String number = read(getAs("/api/tickets/" + id, s.pm()), "$.number");
        ResultActions first = slaCheck(atRisk);
        assertThat(alertKinds(first, id)).containsExactly("AT_RISK");
        first.andExpect(jsonPath("$.alerts[?(@.ticketId=='" + id + "')].assigneeName").value(hasItem("Test DEVELOPER")))
                .andExpect(jsonPath("$.atRisk").value(greaterThanOrEqualTo(1))).andExpect(jsonPath("$.checked").value(greaterThanOrEqualTo(1)));
        assertThat(alertKinds(slaCheck(atRisk), id)).isEmpty();
        assertThat(alertKinds(slaCheck("{\"now\":\"" + created.plus(Duration.ofMinutes(230)) + "\"}"), id)).isEmpty();
        assertThat(countFor(s.dev(), number, "close to its answer deadline")).isEqualTo(1);
        assertThat(countFor(s.pm(), number, "close to its answer deadline")).isEqualTo(1);
        assertThat(countFor(s.client(), number, "deadline")).isZero();

        // past the deadline: breached, once
        String breached = "{\"now\":\"" + created.plus(Duration.ofMinutes(250)) + "\"}";
        assertThat(alertKinds(slaCheck(breached), id)).containsExactly("BREACHED");
        assertThat(alertKinds(slaCheck(breached), id)).isEmpty();
        assertThat(alertKinds(slaCheck("{\"now\":\"" + created.plus(Duration.ofHours(30)) + "\"}"), id)).isEmpty();
        assertThat(countFor(s.dev(), number, "missed its answer deadline")).isEqualTo(1);
        assertThat(countFor(adminToken(), number, "missed its answer deadline")).isEqualTo(1);

        // a ticket that jumps straight to breached only gets the breach; a resolved ticket is left alone
        String skipped = newTicket(s.client(), "LOW", null);
        Instant skippedAt = instantOf(s.pm(), skipped, "createdAt");
        assertThat(alertKinds(slaCheck("{\"now\":\"" + skippedAt.plus(Duration.ofHours(100)) + "\"}"), skipped)).containsExactly("BREACHED");
        assertThat(alertKinds(slaCheck("{\"now\":\"" + skippedAt.plus(Duration.ofHours(101)) + "\"}"), skipped)).isEmpty();
        String done = newTicket(s.client(), "URGENT", null);
        setStatus(s.pm(), done, "RESOLVED").andExpect(status().isOk());
        assertThat(alertKinds(slaCheck("{\"now\":\"" + Instant.now().plus(Duration.ofHours(10)) + "\"}"), done)).isEmpty();

        // a reopened ticket gets a fresh clock and so can alert again
        String again = newTicket(s.client(), "URGENT", null);
        assertThat(alertKinds(slaCheck("{\"now\":\"" + Instant.now().plus(Duration.ofHours(5)) + "\"}"), again)).containsExactly("BREACHED");
        setStatus(s.pm(), again, "RESOLVED").andExpect(status().isOk());
        postJson("/api/tickets/" + again + "/reopen", "{}", s.client()).andExpect(status().isOk());
        assertThat(alertKinds(slaCheck("{\"now\":\"" + Instant.now().plus(Duration.ofMinutes(215)) + "\"}"), again)).containsExactly("AT_RISK");

        // "today" means the start of that day (UTC) and "now" wins when both are given
        String future = newTicket(s.client(), "MEDIUM", null);
        assertThat(alertKinds(slaCheck("{\"today\":\"2100-01-01\"}"), future)).containsExactly("BREACHED");
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Ticket SLA alert")));
    }

    @Test
    void theDashboardShowsEachPersonOnlyTheirOwnCounts() throws Exception {
        Scenario s = scenario();
        String a = newTicket(s.client(), "HIGH", null);
        newTicket(s.client(), "LOW", null);
        postJson("/api/tickets/" + a + "/assign", json("assigneeId", s.devId()), s.pm()).andExpect(status().isOk());
        setStatus(s.dev(), a, "WAITING_CLIENT").andExpect(status().isOk());
        moveDeadline(a, Instant.now().minus(Duration.ofHours(10)), Instant.now().minus(Duration.ofHours(2)));
        newRenewal(s.pm(), s.projectId(), "DOMAIN", "dash1.example", day(5), 25_000);
        newRenewal(s.pm(), s.projectId(), "SSL", "dash2.example", day(-5), 25_000);
        newRenewal(s.pm(), s.projectId(), "HOSTING", "dash3.example", day(200), 25_000);
        sentInvoice(s.pm(), s.projectId(), day(-20), day(-5), 700_000);
        sentInvoice(s.pm(), s.projectId(), day(0), day(10), 300_000);
        draftInvoice(s.pm(), s.projectId(), day(0), day(10), 1000);
        String task = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), task, s.devId(), 10).andExpect(status().isOk());

        getAs("/api/dashboard/stage3", s.client()).andExpect(status().isOk())
                .andExpect(jsonPath("$.invoices.unpaidCount").value(2)).andExpect(jsonPath("$.invoices.unpaidAmount").value(1_000_000))
                .andExpect(jsonPath("$.invoices.overdueCount").value(1)).andExpect(jsonPath("$.invoices.overdueAmount").value(700_000))
                .andExpect(jsonPath("$.invoices.draftCount").doesNotExist())
                .andExpect(jsonPath("$.renewals.expiringCount").value(1)).andExpect(jsonPath("$.renewals.expiredCount").value(1))
                .andExpect(jsonPath("$.tickets.openCount").value(2)).andExpect(jsonPath("$.tickets.waitingOnClientCount").value(1))
                .andExpect(jsonPath("$.tickets.breachedCount").doesNotExist()).andExpect(jsonPath("$.tickets.unassignedCount").doesNotExist());
        getAs("/api/dashboard/stage3", s.dev()).andExpect(status().isOk()).andExpect(jsonPath("$.invoices").doesNotExist())
                .andExpect(jsonPath("$.renewals.expiringCount").value(1)).andExpect(jsonPath("$.tickets.openCount").value(1))
                .andExpect(jsonPath("$.tickets.breachedCount").value(1)).andExpect(jsonPath("$.tickets.unassignedCount").doesNotExist())
                .andExpect(jsonPath("$.tickets.waitingOnClientCount").doesNotExist());
        getAs("/api/dashboard/stage3", s.dev2()).andExpect(jsonPath("$.renewals.expiringCount").value(0)).andExpect(jsonPath("$.tickets.openCount").value(0));
        for (String token : new String[]{s.pm(), adminToken()}) {
            getAs("/api/dashboard/stage3", token).andExpect(status().isOk()).andExpect(jsonPath("$.invoices.draftCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.invoices.unpaidCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.tickets.unassignedCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.tickets.breachedCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.tickets.atRiskCount").exists());
        }
        getAs("/api/dashboard/stage3", null).andExpect(status().isUnauthorized());
    }
}
