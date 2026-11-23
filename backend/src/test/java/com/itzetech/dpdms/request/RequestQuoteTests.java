package com.itzetech.dpdms.request;

import com.itzetech.dpdms.support.DeliveryTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestQuoteTests extends DeliveryTestBase {

    @Test
    void clientSendsARequestAndTheTeamIsTold() throws Exception {
        String pm = newPm();
        String client = newClient("Rwanda Tours");
        String id = sendRequest(client, "Tour booking site");

        getAs("/api/requests/" + id, client).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW")).andExpect(jsonPath("$.quote").value((Object) null))
                .andExpect(jsonPath("$.companyName").value("Rwanda Tours")).andExpect(jsonPath("$.budget").value("RWF 3,000,000"));
        getAs("/api/notifications", pm).andExpect(jsonPath("$.items[*].message", hasItem("New request from Rwanda Tours: Tour booking site")));
        getAs("/api/notifications", adminToken()).andExpect(jsonPath("$.items[*].message", hasItem("New request from Rwanda Tours: Tour booking site")));
    }

    @Test
    void requestFormIsChecked() throws Exception {
        String client = newClient("Form Co");
        postJson("/api/requests", "{\"title\":\"\",\"description\":\"\",\"deadline\":null}", client)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.title").exists()).andExpect(jsonPath("$.fields.description").exists())
                .andExpect(jsonPath("$.fields.deadline").exists());
        postJson("/api/requests", "{\"title\":\"Site\",\"description\":\"Something\",\"deadline\":\"" + day(-1) + "\"}", client)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DEADLINE_PAST"));
        // only clients send requests
        postJson("/api/requests", requestBody("Staff request"), newPm()).andExpect(status().isForbidden());
        postJson("/api/requests", requestBody("Admin request"), adminToken()).andExpect(status().isForbidden());
        postJson("/api/requests", requestBody("Dev request"), newDev()).andExpect(status().isForbidden());
        postJson("/api/requests", requestBody("Anonymous"), "not-a-token").andExpect(status().isUnauthorized());
    }

    @Test
    void whoSeesWhichRequests() throws Exception {
        String pm = newPm();
        String dev = newDev();
        String acme = newClient("Acme Seen");
        String other = newClient("Other Seen");
        String id = sendRequest(acme, "Acme only");

        // management sees every request, a client only their own company's
        getAs("/api/requests", pm).andExpect(jsonPath("$[*].id", hasItem(id)));
        getAs("/api/requests", adminToken()).andExpect(jsonPath("$[*].id", hasItem(id)));
        getAs("/api/requests", acme).andExpect(jsonPath("$[*].id", hasItem(id)));
        getAs("/api/requests", other).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/requests/" + id, other).andExpect(status().isNotFound());
        getAs("/api/requests/" + id, pm).andExpect(status().isOk());

        // developers have nothing to do with requests
        getAs("/api/requests", dev).andExpect(status().isForbidden());
        getAs("/api/requests/" + id, dev).andExpect(status().isForbidden());

        // filter by status
        getAs("/api/requests?status=NEW", acme).andExpect(jsonPath("$[*].id", hasItem(id)));
        getAs("/api/requests?status=ACCEPTED", acme).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/requests?status=NONSENSE", acme).andExpect(status().isBadRequest());
        getAs("/api/requests/not-an-id", acme).andExpect(status().isBadRequest());
        getAs("/api/requests", null).andExpect(status().isUnauthorized());
    }

    @Test
    void projectManagerWritesAQuoteAndTheClientIsTold() throws Exception {
        String pm = newPm();
        String client = newClient("Quote Co");
        String id = sendRequest(client, "Shop");

        postJson("/api/requests/" + id + "/quote", quoteBody(300_000, 450_000), pm).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUOTED")).andExpect(jsonPath("$.quote.total").value(750_000))
                .andExpect(jsonPath("$.quote.weeks").value(8)).andExpect(jsonPath("$.quote.validUntil").value(day(14)))
                .andExpect(jsonPath("$.quote.items", hasSize(2))).andExpect(jsonPath("$.quote.items[0].description").value("Line 1"))
                .andExpect(jsonPath("$.quote.items[1].amount").value(450_000));
        getAs("/api/requests/" + id, client).andExpect(jsonPath("$.quote.total").value(750_000));
        getAs("/api/notifications", client).andExpect(jsonPath("$.items[*].message", hasItem(containsString("sent you a quote for \"Shop\""))));
        getAs("/api/admin/audit?limit=200", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Quote sent")));
    }

    @Test
    void quoteIsChecked() throws Exception {
        String pm = newPm();
        String client = newClient("Check Co");
        String id = sendRequest(client, "Checks");

        postJson("/api/requests/" + id + "/quote", "{\"items\":[],\"weeks\":8,\"validUntil\":\"" + day(5) + "\"}", pm)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.items").exists());
        postJson("/api/requests/" + id + "/quote", "{\"items\":[{\"description\":\"A\",\"amount\":0}],\"weeks\":0,\"validUntil\":null}", pm)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.weeks").exists()).andExpect(jsonPath("$.fields.validUntil").exists());
        postJson("/api/requests/" + id + "/quote", "{\"items\":[{\"description\":\"A\",\"amount\":100}],\"weeks\":4,\"validUntil\":\"" + day(-2) + "\"}", pm)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALID_UNTIL_PAST"));

        // a client or a developer cannot write quotes, and a stranger cannot even see the request
        postJson("/api/requests/" + id + "/quote", quoteBody(100), client).andExpect(status().isForbidden());
        postJson("/api/requests/" + id + "/quote", quoteBody(100), newDev()).andExpect(status().isForbidden());
        postJson("/api/requests/" + id + "/quote", quoteBody(100), pm.substring(0, pm.length() - 3)).andExpect(status().isUnauthorized());
        postJson("/api/requests/00000000-0000-0000-0000-000000000000/quote", quoteBody(100), pm).andExpect(status().isNotFound());
    }

    @Test
    void clientAsksForChangesAndTheQuoteIsRevised() throws Exception {
        String pm = newPm();
        String client = newClient("Change Co");
        String id = sendRequest(client, "Portal");
        sendQuote(pm, id, 900_000);

        postJson("/api/requests/" + id + "/request-changes", "{\"note\":\"\"}", client)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.note").exists());
        postJson("/api/requests/" + id + "/request-changes", json("note", "Please add a mobile app too"), client)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.changeNote").value("Please add a mobile app too"));
        getAs("/api/notifications", pm).andExpect(jsonPath("$.items[*].message", hasItem(containsString("Please add a mobile app too"))));

        // the client cannot accept a quote that is being revised
        postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        postJson("/api/requests/" + id + "/request-changes", json("note", "Again"), client).andExpect(status().isConflict());

        // a revised quote goes back to the client with the old note cleared
        postJson("/api/requests/" + id + "/quote", quoteBody(900_000, 400_000), pm).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUOTED")).andExpect(jsonPath("$.changeNote").value((Object) null))
                .andExpect(jsonPath("$.quote.total").value(1_300_000)).andExpect(jsonPath("$.quote.items", hasSize(2)));
        getAs("/api/notifications", client).andExpect(jsonPath("$.items[*].message", hasItem(containsString("sent you a revised quote"))));
        getAs("/api/admin/audit?limit=200", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Quote revised")));
    }

    @Test
    void acceptingTheQuoteCreatesTheProject() throws Exception {
        String pm = newPm();
        String pm2 = newPm();
        String client = newClient("Accept Co");
        String id = sendRequest(client, "Booking portal");
        sendQuote(pm, id, 1_000_000, 500_000);

        String projectId = read(postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED")).andExpect(jsonPath("$.projectId").exists()), "$.projectId");

        getAs("/api/projects/" + projectId, pm).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Booking portal")).andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.budget").value(1_500_000)).andExpect(jsonPath("$.managerId").value(meId(pm)))
                .andExpect(jsonPath("$.companyName").value("Accept Co")).andExpect(jsonPath("$.progress").value(0))
                .andExpect(jsonPath("$.tasks", hasSize(4)))
                .andExpect(jsonPath("$.tasks[0].title").value("Kick-off")).andExpect(jsonPath("$.tasks[0].assigneeId").value(meId(pm)))
                .andExpect(jsonPath("$.tasks[0].phase").value(0)).andExpect(jsonPath("$.tasks[0].plannedHours").value(8))
                .andExpect(jsonPath("$.tasks[1].title").value("Design")).andExpect(jsonPath("$.tasks[1].assigneeId").doesNotExist())
                .andExpect(jsonPath("$.tasks[1].plannedHours").value(20))
                .andExpect(jsonPath("$.tasks[2].title").value("Development")).andExpect(jsonPath("$.tasks[2].plannedHours").value(60))
                .andExpect(jsonPath("$.tasks[3].title").value("Testing and launch")).andExpect(jsonPath("$.tasks[3].plannedHours").value(16))
                .andExpect(jsonPath("$.tasks[*].status", not(hasItem("DONE"))))
                .andExpect(jsonPath("$.plannedHours").value(104))
                .andExpect(jsonPath("$.phases", hasSize(4))).andExpect(jsonPath("$.phases[0].name").value("Discovery"))
                .andExpect(jsonPath("$.phases[3].name").value("Test and launch"));

        // every project manager is told, and the client's page links to the project
        getAs("/api/notifications", pm2).andExpect(jsonPath("$.items[*].message", hasItem(containsString("Accept Co accepted the quote for \"Booking portal\""))));
        getAs("/api/notifications", pm).andExpect(jsonPath("$.items[*].message", hasItem(containsString("accepted the quote"))));
        getAs("/api/projects/" + projectId + "/updates", client).andExpect(jsonPath("$[*].message", hasItem("Your request was accepted and your project has been set up.")));
        getAs("/api/admin/audit?limit=200", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Quote accepted")));
    }

    @Test
    void aQuoteCanOnlyBeAnsweredOnceAndOnlyByTheRightClient() throws Exception {
        String pm = newPm();
        String client = newClient("Once Co");
        String stranger = newClient("Stranger Co");
        String id = sendRequest(client, "Once");

        // nothing to answer before there is a quote
        postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
        postJson("/api/requests/" + id + "/decline", "{}", client).andExpect(status().isConflict());
        sendQuote(pm, id, 100_000);

        postJson("/api/requests/" + id + "/accept", "{}", stranger).andExpect(status().isNotFound());
        postJson("/api/requests/" + id + "/accept", "{}", pm).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CLIENT_ONLY"));
        postJson("/api/requests/" + id + "/accept", "{}", adminToken()).andExpect(status().isForbidden());
        postJson("/api/requests/" + id + "/accept", "{}", newDev()).andExpect(status().isForbidden());

        postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isOk());
        postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isConflict());
        postJson("/api/requests/" + id + "/decline", "{}", client).andExpect(status().isConflict());
        postJson("/api/requests/" + id + "/request-changes", json("note", "Too late"), client).andExpect(status().isConflict());
        postJson("/api/requests/" + id + "/quote", quoteBody(5), pm).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test
    void decliningCloseTheRequestAndTellsTheTeam() throws Exception {
        String pm = newPm();
        String client = newClient("Decline Co");
        String id = sendRequest(client, "Declined idea");
        sendQuote(pm, id, 250_000);

        postJson("/api/requests/" + id + "/decline", json("reason", "Over our budget"), client)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DECLINED")).andExpect(jsonPath("$.projectId").value((Object) null));
        getAs("/api/notifications", pm).andExpect(jsonPath("$.items[*].message", hasItem(containsString("declined the quote for \"Declined idea\". Reason: Over our budget"))));
        postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isConflict());
        postJson("/api/requests/" + id + "/quote", quoteBody(5), pm).andExpect(status().isConflict());
    }

    @Test
    void anExpiredQuoteCannotBeAccepted() throws Exception {
        String pm = newPm();
        String client = newClient("Late Co");
        String id = sendRequest(client, "Late answer");
        sendQuote(pm, id, 100_000);
        jdbc.update("update client_request set quote_valid_until = ? where id = ?::uuid", java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)), id);

        postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("QUOTE_EXPIRED"));
        // the manager can send a fresh quote
        postJson("/api/requests/" + id + "/quote", quoteBody(100_000), pm).andExpect(status().isOk());
        postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isOk());
    }

    @Test
    void anAdministratorWhoWritesTheQuoteRunsTheProject() throws Exception {
        String pm = newPm();
        String client = newClient("Admin Quote Co");
        String id = sendRequest(client, "Quoted by admin");
        postJson("/api/requests/" + id + "/quote", quoteBody(100_000), adminToken()).andExpect(status().isOk());
        String projectId = read(postJson("/api/requests/" + id + "/accept", "{}", client).andExpect(status().isOk()), "$.projectId");
        // an administrator who writes the quote keeps the project
        getAs("/api/projects/" + projectId, pm).andExpect(jsonPath("$.managerId").value(meId(adminToken())));
    }
}
