package com.itzetech.dpdms.renewal;

import com.itzetech.dpdms.support.MoneyCareTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The reminder tests pick their own far-future expiry dates (years apart) because "run the reminders as if today were X"
 * is global: it looks at every renewal in the database that expires within 60 days of X.
 */
class RenewalTests extends MoneyCareTestBase {

    @Autowired
    ApplicationContext context;

    private ResultActions run(String today) throws Exception {
        return postJson("/api/admin/renewals/run-reminders", "{\"today\":\"" + today + "\"}", adminToken());
    }

    /** The threshold this renewal was reminded for in the run, or -1 when it was not reminded. */
    private int remindedThreshold(ResultActions run, String renewalId) throws Exception {
        String body = run.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> found = JsonPath.read(body, "$.reminders[?(@.renewalId=='" + renewalId + "')].threshold");
        return found.isEmpty() ? -1 : found.get(0);
    }

    @Test
    void theScheduledJobsAreSwitchedOffInTests() {
        assertThat(context.containsBean("jobsConfig")).isFalse();
    }

    @Test
    void statusIsDerivedFromTheExpiryDate() throws Exception {
        Scenario s = scenario();
        String expired = newRenewal(s.pm(), s.projectId(), "DOMAIN", "expired.example", day(-1), 25_000);
        String today = newRenewal(s.pm(), s.projectId(), "SSL", "today.example", day(0), 60_000);
        String edge = newRenewal(s.pm(), s.projectId(), "HOSTING", "edge.example", day(30), 180_000);
        String active = newRenewal(s.pm(), s.projectId(), "MAINTENANCE", "active.example", day(31), 450_000);

        getAs("/api/renewals/" + expired, s.pm()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED")).andExpect(jsonPath("$.daysLeft").value(-1))
                .andExpect(jsonPath("$.type").value("DOMAIN")).andExpect(jsonPath("$.projectName").value("Booking website"))
                .andExpect(jsonPath("$.price").value(25_000)).andExpect(jsonPath("$.note").value("internal")).andExpect(jsonPath("$.history", hasSize(0)))
                .andExpect(jsonPath("$.renewalCount").value(0)).andExpect(jsonPath("$.lastRenewedOn").value((Object) null));
        getAs("/api/renewals/" + today, s.pm()).andExpect(jsonPath("$.status").value("EXPIRING")).andExpect(jsonPath("$.daysLeft").value(0));
        getAs("/api/renewals/" + edge, s.pm()).andExpect(jsonPath("$.status").value("EXPIRING")).andExpect(jsonPath("$.daysLeft").value(30));
        getAs("/api/renewals/" + active, s.pm()).andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.daysLeft").value(31));

        // filters; soonest expiry first
        String project = "projectId=" + s.projectId();
        getAs("/api/renewals?" + project, s.pm()).andExpect(jsonPath("$", hasSize(4))).andExpect(jsonPath("$[0].id").value(expired))
                .andExpect(jsonPath("$[3].id").value(active)).andExpect(jsonPath("$[0].history").doesNotExist());
        getAs("/api/renewals?" + project + "&status=EXPIRING", s.pm()).andExpect(jsonPath("$", hasSize(2)));
        getAs("/api/renewals?" + project + "&status=EXPIRED", s.pm()).andExpect(jsonPath("$", hasSize(1)));
        getAs("/api/renewals?" + project + "&type=SSL", s.pm()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(today));
        getAs("/api/renewals?" + project + "&expiringWithinDays=30", s.pm()).andExpect(jsonPath("$", hasSize(2)));
        getAs("/api/renewals?" + project + "&expiringWithinDays=31", s.pm()).andExpect(jsonPath("$", hasSize(3)));
        getAs("/api/renewals?" + project + "&companyId=" + companyIdOf(s.pm(), s.projectId()), s.pm()).andExpect(jsonPath("$", hasSize(4)));
        getAs("/api/renewals?status=NOPE", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        getAs("/api/renewals?expiringWithinDays=-1", s.pm()).andExpect(status().isBadRequest());
        getAs("/api/projects/" + s.projectId() + "/renewals", s.pm()).andExpect(jsonPath("$", hasSize(4)));
        getAs("/api/projects/" + s.projectId() + "/renewals", s.client()).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void onlyManagementWritesAndInputIsChecked() throws Exception {
        Scenario s = scenario();
        String id = newRenewal(s.pm(), s.projectId(), "DOMAIN", "write.example", day(90), 25_000);
        String body = renewalBody(s.projectId(), "DOMAIN", "x.example", day(90), 1);
        postJson("/api/renewals", body, s.client()).andExpect(status().isForbidden());
        postJson("/api/renewals", body, s.dev()).andExpect(status().isForbidden());
        postJson("/api/renewals", "{}", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.projectId").exists()).andExpect(jsonPath("$.fields.label").exists()).andExpect(jsonPath("$.fields.price").exists());
        postJson("/api/renewals", renewalBody(s.projectId(), "DOMAIN", "x.example", day(90), -5), s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.price").exists());
        postJson("/api/renewals", renewalBody(s.projectId(), "COFFEE", "x.example", day(90), 5), s.pm()).andExpect(status().isBadRequest());
        postJson("/api/renewals", renewalBody("00000000-0000-0000-0000-000000000001", "DOMAIN", "x.example", day(90), 5), s.pm()).andExpect(status().isNotFound());

        putJson("/api/renewals/" + id, "{\"type\":\"HOSTING\",\"label\":\"Pro plan\",\"provider\":\"CloudNest\",\"expiresOn\":\"" + day(120) + "\",\"price\":99000}", s.pm())
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("HOSTING")).andExpect(jsonPath("$.label").value("Pro plan"))
                .andExpect(jsonPath("$.provider").value("CloudNest")).andExpect(jsonPath("$.price").value(99_000)).andExpect(jsonPath("$.expiresOn").value(day(120)));
        putJson("/api/renewals/" + id, "{\"type\":\"HOSTING\",\"label\":\"x\",\"expiresOn\":\"" + day(120) + "\",\"price\":1}", s.client()).andExpect(status().isForbidden());
        putJson("/api/renewals/00000000-0000-0000-0000-000000000001", "{\"type\":\"HOSTING\",\"label\":\"x\",\"expiresOn\":\"" + day(120) + "\",\"price\":1}", s.pm())
                .andExpect(status().isNotFound());
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Renewal added"))).andExpect(jsonPath("$[*].action", hasItem("Renewal updated")));
    }

    @Test
    void renewingRecordsHistoryAndOnlyUnrenewedItemsCanBeDeleted() throws Exception {
        Scenario s = scenario();
        String id = newRenewal(s.pm(), s.projectId(), "DOMAIN", "renew.example", day(10), 25_000);
        String neverRenewed = newRenewal(s.pm(), s.projectId(), "SSL", "gone.example", day(10), 60_000);

        postJson("/api/renewals/" + id + "/renew", "{\"newExpiresOn\":\"" + day(10) + "\"}", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("EXPIRY_NOT_LATER"));
        postJson("/api/renewals/" + id + "/renew", "{}", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.newExpiresOn").exists());
        postJson("/api/renewals/" + id + "/renew", "{\"newExpiresOn\":\"" + day(375) + "\"}", s.client()).andExpect(status().isForbidden());
        long before = countNotifications(s.client(), "has been renewed");
        postJson("/api/renewals/" + id + "/renew", "{\"newExpiresOn\":\"" + day(375) + "\",\"price\":27000,\"note\":\"Paid by mobile money\"}", s.pm())
                .andExpect(status().isOk()).andExpect(jsonPath("$.expiresOn").value(day(375))).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.price").value(27_000)).andExpect(jsonPath("$.renewalCount").value(1)).andExpect(jsonPath("$.lastRenewedOn").value(day(0)))
                .andExpect(jsonPath("$.history", hasSize(1))).andExpect(jsonPath("$.history[0].previousExpiresOn").value(day(10)))
                .andExpect(jsonPath("$.history[0].newExpiresOn").value(day(375))).andExpect(jsonPath("$.history[0].price").value(27_000))
                .andExpect(jsonPath("$.history[0].renewedByName").value("Test PROJECT_MANAGER")).andExpect(jsonPath("$.history[0].note").value("Paid by mobile money"));
        assertThat(countNotifications(s.client(), "has been renewed")).isEqualTo(before + 1);
        postJson("/api/renewals/" + id + "/renew", "{\"newExpiresOn\":\"" + day(740) + "\"}", s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.history", hasSize(2))).andExpect(jsonPath("$.history[0].price").value(27_000));
        getAs("/api/renewals/" + id, s.pm()).andExpect(jsonPath("$.history", hasSize(2)));

        deleteAs("/api/renewals/" + id, s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HAS_HISTORY"));
        deleteAs("/api/renewals/" + neverRenewed, s.client()).andExpect(status().isForbidden());
        deleteAs("/api/renewals/" + neverRenewed, s.pm()).andExpect(status().isNoContent());
        getAs("/api/renewals/" + neverRenewed, s.pm()).andExpect(status().isNotFound());
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Renewal renewed"))).andExpect(jsonPath("$[*].action", hasItem("Renewal deleted")));
    }

    @Test
    void clientsAndDevelopersSeeRenewalsThroughTheirOwnLens() throws Exception {
        Scenario s = scenario();
        Scenario other = scenario();
        String mine = newRenewal(s.pm(), s.projectId(), "DOMAIN", "mine.example", day(100), 25_000);
        String theirs = newRenewal(other.pm(), other.projectId(), "DOMAIN", "theirs.example", day(100), 25_000);
        String task = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), task, s.devId(), 10).andExpect(status().isOk());

        // client: own company only, sees the price, never the internal note, cannot write
        getAs("/api/renewals", s.client()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(mine))
                .andExpect(jsonPath("$[0].price").value(25_000)).andExpect(jsonPath("$[0].note").doesNotExist());
        getAs("/api/renewals/" + mine, s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.note").doesNotExist());
        getAs("/api/renewals/" + theirs, s.client()).andExpect(status().isNotFound());
        getAs("/api/projects/" + other.projectId() + "/renewals", s.client()).andExpect(status().isNotFound());
        deleteAs("/api/renewals/" + mine, s.client()).andExpect(status().isForbidden());
        postJson("/api/renewals/" + mine + "/renew", "{\"newExpiresOn\":\"" + day(400) + "\"}", s.client()).andExpect(status().isForbidden());

        // developer: read only, projects they can see, no price
        getAs("/api/renewals", s.dev()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(mine)).andExpect(jsonPath("$[0].price").doesNotExist())
                .andExpect(jsonPath("$[0].note").value("internal"));
        getAs("/api/renewals/" + mine, s.dev()).andExpect(status().isOk()).andExpect(jsonPath("$.price").doesNotExist());
        getAs("/api/renewals/" + mine, s.dev2()).andExpect(status().isNotFound());
        getAs("/api/renewals", s.dev2()).andExpect(jsonPath("$", hasSize(0)));
        deleteAs("/api/renewals/" + mine, s.dev()).andExpect(status().isForbidden());
        getAs("/api/renewals", null).andExpect(status().isUnauthorized());

        // a client can ask for a renewal; only their own, and management hears about it
        long before = countNotifications(s.pm(), "asks you to renew");
        postJson("/api/renewals/" + mine + "/request-renewal", "{}", s.client()).andExpect(status().isAccepted());
        assertThat(countNotifications(adminToken(), "asks you to renew")).isGreaterThan(0);
        assertThat(countNotifications(s.pm(), "asks you to renew")).isEqualTo(before + 1);
        postJson("/api/renewals/" + theirs + "/request-renewal", "{}", s.client()).andExpect(status().isNotFound());
        postJson("/api/renewals/" + mine + "/request-renewal", "{}", s.pm()).andExpect(status().isForbidden());
        postJson("/api/renewals/" + mine + "/request-renewal", "{}", s.dev()).andExpect(status().isForbidden());
    }

    @Test
    void remindersGoOutAtEachThresholdExactlyOnce() throws Exception {
        Scenario s = scenario();
        String id = newRenewal(s.pm(), s.projectId(), "DOMAIN", "remind.example", day(3000), 25_000);
        String projectClient = s.client();
        long clientBefore = countNotifications(projectClient, "remind.example");
        long pmBefore = countNotifications(s.pm(), "remind.example");

        assertThat(remindedThreshold(run(day(2930)), id)).isEqualTo(-1);   // 70 days left: too early
        assertThat(remindedThreshold(run(day(2940)), id)).isEqualTo(60);   // 60 days left
        assertThat(remindedThreshold(run(day(2940)), id)).isEqualTo(-1);   // same day again: nothing new
        assertThat(remindedThreshold(run(day(2945)), id)).isEqualTo(-1);   // 55 days left: still the 60 reminder
        assertThat(remindedThreshold(run(day(2970)), id)).isEqualTo(30);
        assertThat(remindedThreshold(run(day(2970)), id)).isEqualTo(-1);
        assertThat(remindedThreshold(run(day(2986)), id)).isEqualTo(14);
        assertThat(remindedThreshold(run(day(2993)), id)).isEqualTo(7);
        assertThat(remindedThreshold(run(day(2996)), id)).isEqualTo(-1);   // 4 days left
        assertThat(remindedThreshold(run(day(3000)), id)).isEqualTo(0);    // on expiry
        assertThat(remindedThreshold(run(day(3000)), id)).isEqualTo(-1);
        assertThat(remindedThreshold(run(day(3010)), id)).isEqualTo(-1);   // expired for days: nothing more

        // five reminders reached both the client company and management, once each
        assertThat(countNotifications(projectClient, "remind.example")).isEqualTo(clientBefore + 5);
        assertThat(countNotifications(s.pm(), "remind.example")).isEqualTo(pmBefore + 5);
        assertThat(countNotifications(adminToken(), "remind.example")).isGreaterThanOrEqualTo(5);
        // but not the developers
        assertThat(countNotifications(s.dev(), "remind.example")).isZero();
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Renewal reminder sent")));
    }

    @Test
    void aRenewalRegisteredLateOnlyGetsTheNearestReminderAndRenewingStartsOver() throws Exception {
        Scenario s = scenario();
        String id = newRenewal(s.pm(), s.projectId(), "SSL", "late.example", day(4020), 60_000);
        // registered with 20 days to go: the "30 days" reminder, once, not 60 and 30 together
        ResultActions first = run(day(4000));
        assertThat(remindedThreshold(first, id)).isEqualTo(30);
        first.andExpect(jsonPath("$.today").value(day(4000))).andExpect(jsonPath("$.sent").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.checked").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        assertThat(remindedThreshold(run(day(4000)), id)).isEqualTo(-1);
        assertThat(remindedThreshold(run(day(4001)), id)).isEqualTo(-1);
        assertThat(remindedThreshold(run(day(4015)), id)).isEqualTo(7);

        // renewing moves the expiry: the thresholds count again from the new date
        postJson("/api/renewals/" + id + "/renew", "{\"newExpiresOn\":\"" + day(6000) + "\"}", s.pm()).andExpect(status().isOk());
        assertThat(remindedThreshold(run(day(4020)), id)).isEqualTo(-1);
        assertThat(remindedThreshold(run(day(5940)), id)).isEqualTo(60);
        assertThat(remindedThreshold(run(day(5940)), id)).isEqualTo(-1);

        // a renewal that was already expired when it was registered is told about once
        String expired = newRenewal(s.pm(), s.projectId(), "HOSTING", "gone-long-ago.example", day(7000), 1);
        assertThat(remindedThreshold(run(day(7005)), expired)).isEqualTo(0);
        assertThat(remindedThreshold(run(day(7006)), expired)).isEqualTo(-1);
    }

    @Test
    void theReminderEndpointIsForAdministratorsOnlyAndChecksTheDate() throws Exception {
        Scenario s = scenario();
        postJson("/api/admin/renewals/run-reminders", "{}", s.pm()).andExpect(status().isForbidden());
        postJson("/api/admin/renewals/run-reminders", "{}", s.client()).andExpect(status().isForbidden());
        postJson("/api/admin/renewals/run-reminders", "{}", s.dev()).andExpect(status().isForbidden());
        postJson("/api/admin/renewals/run-reminders", "{}", "bad").andExpect(status().isUnauthorized());
        postJson("/api/admin/renewals/run-reminders", "{\"today\":\"soon\"}", adminToken()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        // no body and an empty body both mean "today"
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/renewals/run-reminders")
                .header("Authorization", "Bearer " + adminToken())).andExpect(status().isOk()).andExpect(jsonPath("$.today").value(day(0)));
        postJson("/api/admin/renewals/run-reminders", "{}", adminToken()).andExpect(status().isOk()).andExpect(jsonPath("$.today").value(day(0)))
                .andExpect(jsonPath("$.reminders", not(org.hamcrest.Matchers.nullValue())));
    }
}
