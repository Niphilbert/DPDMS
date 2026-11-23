package com.itzetech.dpdms.project;

import com.itzetech.dpdms.support.DeliveryTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApprovalTests extends DeliveryTestBase {

    private static String item(String name, String taskId) {
        return "{\"item\":\"" + name + "\",\"type\":\"DESIGN\",\"file\":\"https://figma.example/" + name.replace(' ', '-')
                + "\",\"message\":\"Please have a look\"" + (taskId == null ? "" : ",\"taskId\":\"" + taskId + "\"") + "}";
    }

    @Test
    void managerSendsItemsForApprovalAndVersionsCountUp() throws Exception {
        Scenario s = scenario();
        String url = "/api/projects/" + s.projectId() + "/approvals";

        postJson(url, item("Home page design", null), s.pm()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.type").value("DESIGN")).andExpect(jsonPath("$.item").value("Home page design"))
                .andExpect(jsonPath("$.file").value("https://figma.example/Home-page-design")).andExpect(jsonPath("$.message").value("Please have a look"))
                .andExpect(jsonPath("$.sentByName").value("Test PROJECT_MANAGER"));
        getAs("/api/notifications", s.client()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("sent you \"Home page design (version 1)\" to approve"))));
        getAs("/api/projects/" + s.projectId() + "/updates", s.client()).andExpect(jsonPath("$[*].message", hasItem("Waiting for your approval: Home page design (version 1)")));

        // the same item again, even with different capital letters, is the next version
        postJson(url, item("HOME PAGE DESIGN", null), s.pm()).andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(2));
        postJson(url, item("Logo", null), s.pm()).andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(1));
        postJson(url, "{\"item\":\"Terms\",\"type\":\"DOCUMENT\",\"file\":\"terms.pdf\"}", s.pm()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DOCUMENT")).andExpect(jsonPath("$.message").value((Object) null));

        for (String token : new String[]{s.client(), s.pm(), adminToken()}) {
            getAs(url, token).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)))
                    .andExpect(jsonPath("$[0].item").value("Terms")).andExpect(jsonPath("$[3].version").value(1));
        }
        getAs("/api/projects/" + s.projectId(), s.client()).andExpect(jsonPath("$.openApprovals").value(4));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Approval requested")));
    }

    @Test
    void sendingAnItemIsChecked() throws Exception {
        Scenario s = scenario();
        Scenario other = scenario();
        String url = "/api/projects/" + s.projectId() + "/approvals";

        postJson(url, "{\"item\":\"\",\"file\":\"\"}", s.pm()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(jsonPath("$.fields.item").exists())
                .andExpect(jsonPath("$.fields.type").exists()).andExpect(jsonPath("$.fields.file").exists());
        postJson(url, "{\"item\":\"Logo\",\"type\":\"VIDEO\",\"file\":\"x\"}", s.pm()).andExpect(status().isBadRequest());
        // a task from another project cannot be linked
        String foreignTask = taskId(other.pm(), other.projectId(), "Design");
        postJson(url, item("Logo", foreignTask), s.pm()).andExpect(status().isNotFound());
        postJson(url, item("Logo", null), s.client()).andExpect(status().isForbidden());
        postJson(url, item("Logo", null), other.pm()).andExpect(status().isCreated()); // any project manager may send items
        postJson(url, item("Logo", null), other.client()).andExpect(status().isForbidden());
        postJson(url, item("Logo", null), "bad-token").andExpect(status().isUnauthorized());
        getAs(url, other.client()).andExpect(status().isNotFound());
    }

    @Test
    void clientApprovesAnItemAndItsTaskIsMarkedDone() throws Exception {
        Scenario s = scenario();
        Scenario other = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());
        startProject(s.pm(), s.projectId());
        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isOk());
        submitTask(s.dev(), s.projectId(), design).andExpect(status().isOk());

        String approvalId = read(postJson("/api/projects/" + s.projectId() + "/approvals", item("Design mock-up", design), s.pm())
                .andExpect(status().isCreated()), "$.id");
        String base = "/api/projects/" + s.projectId() + "/approvals/" + approvalId;

        // only the client of this project answers
        postJson(base + "/approve", "{}", other.client()).andExpect(status().isNotFound());
        postJson(base + "/approve", "{}", s.pm()).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CLIENT_ONLY"));
        postJson(base + "/approve", "{}", adminToken()).andExpect(status().isForbidden());
        postJson(base + "/approve", "{}", s.dev()).andExpect(status().isForbidden());
        postJson("/api/projects/" + s.projectId() + "/approvals/00000000-0000-0000-0000-000000000000/approve", "{}", s.client()).andExpect(status().isNotFound());
        getAs("/api/projects/" + s.projectId(), s.client()).andExpect(jsonPath("$.tasks[?(@.title=='Design')].status").value("REVIEW"));

        postJson(base + "/approve", json("note", "Looks great"), s.client()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.answerNote").value("Looks great"))
                .andExpect(jsonPath("$.answeredByName").value(org.hamcrest.Matchers.startsWith("Client "))).andExpect(jsonPath("$.answeredAt").exists());

        // the linked task is finished: 20 of 104 hours
        getAs("/api/projects/" + s.projectId(), s.pm()).andExpect(jsonPath("$.tasks[?(@.title=='Design')].status").value("DONE"))
                .andExpect(jsonPath("$.progress").value(19)).andExpect(jsonPath("$.openApprovals").value(0));
        getAs("/api/notifications", s.pm()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("approved \"Design mock-up (version 1)\""))));
        getAs("/api/notifications", s.dev()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("\"Design\" in \"Booking website\" was marked done"))));
        getAs("/api/projects/" + s.projectId() + "/updates", s.client()).andExpect(jsonPath("$[*].message", hasItem("Approved: Design mock-up (version 1)")))
                .andExpect(jsonPath("$[*].message", hasItem("Finished: Design")));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Approval given")));

        // answered once, for good
        postJson(base + "/approve", "{}", s.client()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_ANSWERED"));
        postJson(base + "/request-changes", json("note", "Changed my mind"), s.client()).andExpect(status().isConflict());
    }

    @Test
    void approvingAnItemWithoutAnyBodyWorks() throws Exception {
        Scenario s = scenario();
        String approvalId = read(postJson("/api/projects/" + s.projectId() + "/approvals", item("Brand guide", null), s.pm())
                .andExpect(status().isCreated()), "$.id");
        mvc.perform(post("/api/projects/" + s.projectId() + "/approvals/" + approvalId + "/approve").header("Authorization", "Bearer " + s.client()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.answerNote").value((Object) null));
        // an item that is not tied to a task changes no task
        getAs("/api/projects/" + s.projectId(), s.client()).andExpect(jsonPath("$.progress").value(0));
    }

    @Test
    void clientAsksForChangesAndTheManagerSendsANewVersion() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        String url = "/api/projects/" + s.projectId() + "/approvals";
        String first = read(postJson(url, item("Landing page", design), s.pm()).andExpect(status().isCreated()), "$.id");

        postJson(url + "/" + first + "/request-changes", "{\"note\":\"\"}", s.client()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.note").exists());
        postJson(url + "/" + first + "/request-changes", json("note", "Make the logo bigger"), s.pm()).andExpect(status().isForbidden());
        postJson(url + "/" + first + "/request-changes", json("note", "Make the logo bigger"), s.client()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED")).andExpect(jsonPath("$.answerNote").value("Make the logo bigger"));
        getAs("/api/notifications", s.pm()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("asked for changes to \"Landing page (version 1)\""))))
                .andExpect(jsonPath("$.items[*].message", hasItem(containsString("Make the logo bigger"))));
        getAs("/api/projects/" + s.projectId() + "/updates", s.client()).andExpect(jsonPath("$[*].message", hasItem("Changes requested: Landing page (version 1)")));
        // the task stays as it was
        getAs("/api/projects/" + s.projectId(), s.pm()).andExpect(jsonPath("$.tasks[?(@.title=='Design')].status").value("TODO"));

        String second = read(postJson(url, item("Landing page", design), s.pm()).andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(2)), "$.id");
        postJson(url + "/" + second + "/approve", "{}", s.client()).andExpect(status().isOk());
        getAs(url, s.client()).andExpect(jsonPath("$[0].status").value("APPROVED")).andExpect(jsonPath("$[1].status").value("CHANGES_REQUESTED"));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Changes requested on approval")));
    }

    @Test
    void sendersOtherThanTheManagerAreToldToo() throws Exception {
        Scenario s = scenario();
        String otherPm = newPm();
        String url = "/api/projects/" + s.projectId() + "/approvals";
        String id = read(postJson(url, item("Pricing page", null), otherPm).andExpect(status().isCreated()), "$.id");
        postJson(url + "/" + id + "/approve", "{}", s.client()).andExpect(status().isOk());
        getAs("/api/notifications", otherPm).andExpect(jsonPath("$.items[*].message", hasItem(containsString("approved \"Pricing page (version 1)\""))));
        getAs("/api/notifications", s.pm()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("approved \"Pricing page (version 1)\""))));
    }

    @Test
    void changeRequestsAreProposedByTheTeamAndAnsweredByTheClient() throws Exception {
        Scenario s = scenario();
        Scenario other = scenario();
        String url = "/api/projects/" + s.projectId() + "/change-requests";

        String accepted = read(postJson(url, "{\"title\":\"Add loyalty module\",\"cost\":400000}", s.pm()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROPOSED")).andExpect(jsonPath("$.cost").value(400000))
                .andExpect(jsonPath("$.proposedByName").value("Test PROJECT_MANAGER")), "$.id");
        String declined = read(postJson(url, "{\"title\":\"Add chat widget\",\"cost\":150000}", s.pm()).andExpect(status().isCreated()), "$.id");
        getAs("/api/notifications", s.client()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("Add loyalty module (400,000 RWF)"))));
        getAs("/api/projects/" + s.projectId() + "/updates", s.client()).andExpect(jsonPath("$[*].message", hasItem("Proposed change: Add loyalty module (400,000 RWF)")));

        // lists: management and the client of this project only
        for (String token : new String[]{s.client(), s.pm(), adminToken()}) {
            getAs(url, token).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].title").value("Add chat widget"));
        }
        getAs(url, other.client()).andExpect(status().isNotFound());
        getAs(url, s.dev()).andExpect(status().isNotFound()); // no access to the project at all

        // checks
        postJson(url, "{\"title\":\"\",\"cost\":0}", s.pm()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists()).andExpect(jsonPath("$.fields.cost").exists());
        postJson(url, "{\"title\":\"Free\"}", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.cost").exists());
        postJson(url, "{\"title\":\"By the client\",\"cost\":100}", s.client()).andExpect(status().isForbidden());

        // the team cannot answer for the client, another company cannot answer at all
        postJson(url + "/" + accepted + "/accept", "{}", s.pm()).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CLIENT_ONLY"));
        postJson(url + "/" + accepted + "/accept", "{}", other.client()).andExpect(status().isNotFound());
        postJson(url + "/00000000-0000-0000-0000-000000000000/accept", "{}", s.client()).andExpect(status().isNotFound());

        // accepting adds the cost to the budget; declining does not
        getAs("/api/projects/" + s.projectId(), s.client()).andExpect(jsonPath("$.budget").value(1_500_000));
        postJson(url + "/" + accepted + "/accept", "{}", s.client()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED")).andExpect(jsonPath("$.answeredByName").exists());
        getAs("/api/projects/" + s.projectId(), s.client()).andExpect(jsonPath("$.budget").value(1_900_000));
        postJson(url + "/" + declined + "/decline", "{}", s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DECLINED"));
        getAs("/api/projects/" + s.projectId(), s.pm()).andExpect(jsonPath("$.budget").value(1_900_000));

        // each is answered once
        postJson(url + "/" + accepted + "/decline", "{}", s.client()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_ANSWERED"));
        postJson(url + "/" + declined + "/accept", "{}", s.client()).andExpect(status().isConflict());
        getAs("/api/notifications", s.pm()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("accepted the change request \"Add loyalty module\""))))
                .andExpect(jsonPath("$.items[*].message", hasItem(containsString("declined the change request \"Add chat widget\""))));
        getAs("/api/projects/" + s.projectId() + "/updates", s.client()).andExpect(jsonPath("$[*].message", hasItem("Change accepted: Add loyalty module")))
                .andExpect(jsonPath("$[*].message", hasItem("Change declined: Add chat widget")));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Change request sent")))
                .andExpect(jsonPath("$[*].action", hasItem("Change request accepted"))).andExpect(jsonPath("$[*].action", hasItem("Change request declined")));
    }
}
