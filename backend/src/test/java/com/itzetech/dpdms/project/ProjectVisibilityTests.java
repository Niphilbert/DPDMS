package com.itzetech.dpdms.project;

import com.itzetech.dpdms.support.DeliveryTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectVisibilityTests extends DeliveryTestBase {

    @Test
    void developersSeeOnlyProjectsWhereTheyHaveATask() throws Exception {
        Scenario a = scenario();
        Scenario b = scenario();
        assign(a.pm(), a.projectId(), taskId(a.pm(), a.projectId(), "Design"), a.devId(), 10).andExpect(status().isOk());

        getAs("/api/projects", a.dev()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(a.projectId())).andExpect(jsonPath("$[0].tasks").doesNotExist());
        getAs("/api/projects/" + a.projectId(), a.dev()).andExpect(status().isOk()).andExpect(jsonPath("$.tasks", hasSize(4)));
        // someone else's project looks as if it does not exist; so does one with no task for this developer
        getAs("/api/projects/" + b.projectId(), a.dev()).andExpect(status().isNotFound());
        getAs("/api/projects", a.dev2()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/projects/" + a.projectId(), a.dev2()).andExpect(status().isNotFound());
        getAs("/api/projects/" + a.projectId() + "/updates", a.dev2()).andExpect(status().isNotFound());
        getAs("/api/projects/" + a.projectId() + "/time", a.dev2()).andExpect(status().isNotFound());
        getAs("/api/projects/" + a.projectId() + "/approvals", a.dev2()).andExpect(status().isNotFound());
        getAs("/api/projects/00000000-0000-0000-0000-000000000000", a.pm()).andExpect(status().isNotFound());
        getAs("/api/projects/not-a-uuid", a.pm()).andExpect(status().isBadRequest());

        // management sees everything
        getAs("/api/projects", a.pm()).andExpect(jsonPath("$[*].id", hasItem(a.projectId()))).andExpect(jsonPath("$[*].id", hasItem(b.projectId())));
        getAs("/api/projects", adminToken()).andExpect(jsonPath("$[*].id", hasItem(a.projectId()))).andExpect(jsonPath("$[*].id", hasItem(b.projectId())));
        getAs("/api/projects", null).andExpect(status().isUnauthorized());
    }

    @Test
    void administratorGrantsAndRemovesDeveloperAccess() throws Exception {
        Scenario s = scenario();
        String url = "/api/admin/projects/" + s.projectId() + "/access";
        getAs(url, adminToken()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        postJson(url, json("userId", s.dev2Id()), adminToken()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(s.dev2Id())).andExpect(jsonPath("$[0].fullName").value("Test DEVELOPER"));
        // granting twice changes nothing
        postJson(url, json("userId", s.dev2Id()), adminToken()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        getAs("/api/notifications", s.dev2()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("You can now see the project \"Booking website\""))));
        getAs("/api/projects", s.dev2()).andExpect(jsonPath("$", hasSize(1)));
        getAs("/api/projects/" + s.projectId(), s.dev2()).andExpect(status().isOk());
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Project access granted")));

        // only the administrator decides who gets access
        for (String token : new String[]{s.pm(), s.dev(), s.client()}) {
            getAs(url, token).andExpect(status().isForbidden());
            postJson(url, json("userId", s.devId()), token).andExpect(status().isForbidden());
            deleteAs(url + "/" + s.dev2Id(), token).andExpect(status().isForbidden());
        }
        getAs(url, null).andExpect(status().isUnauthorized());

        // only active developers can be given access
        postJson(url, json("userId", s.pmId()), adminToken()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_A_DEVELOPER"));
        postJson(url, json("userId", s.clientId()), adminToken()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_A_DEVELOPER"));
        postJson(url, json("userId", "00000000-0000-0000-0000-000000000000"), adminToken()).andExpect(status().isNotFound());
        postJson(url, "{}", adminToken()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.userId").exists());
        postJson("/api/admin/projects/00000000-0000-0000-0000-000000000000/access", json("userId", s.devId()), adminToken()).andExpect(status().isNotFound());

        deleteAs(url + "/" + s.dev2Id(), adminToken()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/projects/" + s.projectId(), s.dev2()).andExpect(status().isNotFound());
        getAs("/api/notifications", s.dev2()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("access to the project \"Booking website\" was removed"))));
        deleteAs(url + "/" + s.dev2Id(), adminToken()).andExpect(status().isNotFound());
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Project access removed")));
    }

    @Test
    void removingGrantedAccessKeepsTheProjectForADeveloperWhoHasATask() throws Exception {
        Scenario s = scenario();
        String url = "/api/admin/projects/" + s.projectId() + "/access";
        assign(s.pm(), s.projectId(), taskId(s.pm(), s.projectId(), "Design"), s.devId(), 10).andExpect(status().isOk());
        postJson(url, json("userId", s.devId()), adminToken()).andExpect(status().isOk());
        deleteAs(url + "/" + s.devId(), adminToken()).andExpect(status().isOk());
        getAs("/api/projects/" + s.projectId(), s.dev()).andExpect(status().isOk());
    }

    @Test
    void developersNeverReceiveMoneyOrClientBusiness() throws Exception {
        Scenario s = scenario();
        assign(s.pm(), s.projectId(), taskId(s.pm(), s.projectId(), "Design"), s.devId(), 10).andExpect(status().isOk());
        postJson("/api/projects/" + s.projectId() + "/change-requests", "{\"title\":\"Extra pages\",\"cost\":250000}", s.pm())
                .andExpect(status().isCreated());

        // the budget is not in the project, the list, or the task lists
        getAs("/api/projects/" + s.projectId(), s.dev()).andExpect(status().isOk()).andExpect(jsonPath("$.budget").doesNotExist())
                .andExpect(jsonPath("$.plannedHours").value(104)).andExpect(jsonPath("$.openApprovals").doesNotExist())
                .andExpect(content().string(not(containsString("1500000")))).andExpect(content().string(not(containsString("budget"))))
                .andExpect(content().string(not(containsString("250000"))));
        getAs("/api/projects", s.dev()).andExpect(jsonPath("$[0].budget").doesNotExist());
        getAs("/api/tasks/mine", s.dev()).andExpect(content().string(not(containsString("budget"))));

        // and the places where money lives are closed to them
        postJson("/api/projects/" + s.projectId() + "/approvals",
                "{\"item\":\"Logo\",\"type\":\"DESIGN\",\"file\":\"logo.png\"}", s.dev()).andExpect(status().isForbidden());
        getAs("/api/projects/" + s.projectId() + "/approvals", s.dev()).andExpect(status().isForbidden());
        getAs("/api/projects/" + s.projectId() + "/change-requests", s.dev()).andExpect(status().isForbidden());
        postJson("/api/projects/" + s.projectId() + "/change-requests", "{\"title\":\"X\",\"cost\":10}", s.dev()).andExpect(status().isForbidden());
        getAs("/api/requests", s.dev()).andExpect(status().isForbidden());
        postJson("/api/requests", requestBody("Dev request"), s.dev()).andExpect(status().isForbidden());
        postJson("/api/requests/" + s.requestId() + "/quote", quoteBody(10), s.dev()).andExpect(status().isForbidden());
        getAs("/api/admin/users", s.dev()).andExpect(status().isForbidden());
    }

    @Test
    void clientsSeeOnlyTheirOwnCompanyAndASafeTaskList() throws Exception {
        Scenario a = scenario();
        Scenario b = scenario();
        String design = taskId(a.pm(), a.projectId(), "Design");
        assign(a.pm(), a.projectId(), design, a.devId(), 10).andExpect(status().isOk());
        startProject(a.pm(), a.projectId());
        moveTask(a.dev(), a.projectId(), design, "DOING").andExpect(status().isOk());
        submitTask(a.dev(), a.projectId(), design).andExpect(status().isOk());

        getAs("/api/projects/" + a.projectId(), a.client()).andExpect(status().isOk())
                .andExpect(jsonPath("$.budget").value(1_500_000)).andExpect(jsonPath("$.name").value("Booking website"))
                .andExpect(jsonPath("$.progress").value(0)).andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.tasks", hasSize(4))).andExpect(jsonPath("$.tasks[*].title", hasItem("Design")))
                .andExpect(jsonPath("$.tasks[?(@.title=='Design')].status").value("REVIEW"))
                .andExpect(jsonPath("$.tasks[*].dueDate").exists()).andExpect(jsonPath("$.tasks[*].phase").exists())
                // no people, hours, files or comments for the client
                .andExpect(jsonPath("$.tasks[*].assigneeId").doesNotExist()).andExpect(jsonPath("$.tasks[*].assigneeName").doesNotExist())
                .andExpect(jsonPath("$.tasks[*].plannedHours").doesNotExist()).andExpect(jsonPath("$.tasks[*].loggedHours").doesNotExist())
                .andExpect(jsonPath("$.tasks[*].submissionFile").doesNotExist()).andExpect(jsonPath("$.tasks[*].submissionNote").doesNotExist())
                .andExpect(jsonPath("$.tasks[*].sentBackNote").doesNotExist())
                .andExpect(jsonPath("$.plannedHours").doesNotExist()).andExpect(jsonPath("$.loggedHours").doesNotExist())
                .andExpect(jsonPath("$.requestId").doesNotExist())
                .andExpect(content().string(not(containsString("files.example"))));

        getAs("/api/projects", a.client()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(a.projectId()));
        getAs("/api/projects", b.client()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(b.projectId()));
        for (String url : new String[]{"", "/updates", "/approvals", "/change-requests"}) {
            getAs("/api/projects/" + a.projectId() + url, b.client()).andExpect(status().isNotFound());
        }

        // clients cannot run the project
        String url = "/api/projects/" + a.projectId();
        postJson(url + "/start", "{}", a.client()).andExpect(status().isForbidden());
        postJson(url + "/tasks", "{\"title\":\"X\",\"phase\":1,\"plannedHours\":2,\"dueDate\":\"" + day(5) + "\"}", a.client()).andExpect(status().isForbidden());
        getAs(url + "/time", a.client()).andExpect(status().isForbidden());
        getAs(url + "/tasks/" + design + "/time", a.client()).andExpect(status().isForbidden());
        postJson(url + "/tasks/" + design + "/time", "{\"date\":\"" + day(0) + "\",\"hours\":1}", a.client()).andExpect(status().isForbidden());
        postJson(url + "/tasks/" + design + "/time", "{\"date\":\"" + day(0) + "\",\"hours\":1}", b.client()).andExpect(status().isNotFound());
        getAs("/api/admin/projects/" + a.projectId() + "/access", a.client()).andExpect(status().isForbidden());
    }

    @Test
    void theUpdatesFeedIsReadableByEveryonePermittedOnTheProject() throws Exception {
        Scenario s = scenario();
        assign(s.pm(), s.projectId(), taskId(s.pm(), s.projectId(), "Design"), s.devId(), 10).andExpect(status().isOk());
        startProject(s.pm(), s.projectId());
        for (String token : new String[]{s.client(), s.dev(), s.pm(), adminToken()}) {
            getAs("/api/projects/" + s.projectId() + "/updates", token).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].message").value("Work has started on your project."))
                    .andExpect(jsonPath("$[1].message").value("Your request was accepted and your project has been set up."))
                    .andExpect(jsonPath("$[0].createdAt").exists());
        }
        getAs("/api/projects/" + s.projectId() + "/updates", newClient("Nosy Co")).andExpect(status().isNotFound());
        getAs("/api/projects/" + s.projectId() + "/updates", s.dev2()).andExpect(status().isNotFound());
        getAs("/api/projects/" + s.projectId() + "/updates", null).andExpect(status().isUnauthorized());
    }
}
