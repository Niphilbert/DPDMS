package com.itzetech.dpdms.project;

import com.itzetech.dpdms.support.DeliveryTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TimeLogTests extends DeliveryTestBase {

    private static String entry(int daysAgo, String hours, String note) {
        return "{\"date\":\"" + day(-daysAgo) + "\",\"hours\":" + hours + (note == null ? "" : ",\"note\":\"" + note + "\"") + "}";
    }

    @Test
    void developersLogHoursAgainstTheirOwnTasks() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        String development = taskId(s.pm(), s.projectId(), "Development");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());
        assign(s.pm(), s.projectId(), development, s.dev2Id(), 20).andExpect(status().isOk());
        String timeUrl = "/api/projects/" + s.projectId() + "/tasks/" + design + "/time";

        // not before the project has started
        postJson(timeUrl, entry(0, "2", null), s.dev()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PROJECT_NOT_STARTED"));
        startProject(s.pm(), s.projectId());

        postJson(timeUrl, entry(1, "2.5", "Wireframes"), s.dev()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.entry.hours").value(2.5)).andExpect(jsonPath("$.entry.note").value("Wireframes"))
                .andExpect(jsonPath("$.entry.date").value(day(-1))).andExpect(jsonPath("$.entry.taskTitle").value("Design"))
                .andExpect(jsonPath("$.entry.userName").value("Test DEVELOPER")).andExpect(jsonPath("$.entry.userId").value(s.devId()))
                .andExpect(jsonPath("$.taskLoggedHours").value(2.5)).andExpect(jsonPath("$.taskPlannedHours").value(20));
        postJson(timeUrl, entry(0, "1.25", null), s.dev()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskLoggedHours").value(3.75)).andExpect(jsonPath("$.entry.note").value((Object) null));
        postJson("/api/projects/" + s.projectId() + "/tasks/" + development + "/time", entry(0, "8", "Setup"), s.dev2()).andExpect(status().isCreated());

        // totals: planned against logged, for the manager and for the administrator
        for (String token : new String[]{s.pm(), adminToken()}) {
            getAs("/api/projects/" + s.projectId() + "/time", token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.plannedHours").value(104)).andExpect(jsonPath("$.loggedHours").value(11.75))
                    .andExpect(jsonPath("$.tasks", hasSize(4)))
                    .andExpect(jsonPath("$.tasks[?(@.title=='Design')].loggedHours").value(3.75))
                    .andExpect(jsonPath("$.tasks[?(@.title=='Design')].plannedHours").value(20))
                    .andExpect(jsonPath("$.tasks[?(@.title=='Development')].loggedHours").value(8.0))
                    .andExpect(jsonPath("$.entries", hasSize(3)));
        }
        getAs("/api/projects/" + s.projectId(), s.pm()).andExpect(jsonPath("$.loggedHours").value(11.75))
                .andExpect(jsonPath("$.tasks[?(@.title=='Design')].loggedHours").value(3.75));

        // a developer sees the totals of their own tasks only
        getAs("/api/projects/" + s.projectId() + "/time", s.dev()).andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedHours").value(20)).andExpect(jsonPath("$.loggedHours").value(3.75))
                .andExpect(jsonPath("$.tasks", hasSize(1))).andExpect(jsonPath("$.entries", hasSize(2)));
        getAs("/api/projects/" + s.projectId() + "/time", s.dev2()).andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.loggedHours").value(8.0));

        // the log of one task
        getAs(timeUrl, s.dev()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].date").value(day(0)));
        getAs(timeUrl, s.pm()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        getAs(timeUrl, s.dev2()).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_YOUR_TASK"));
        getAs(timeUrl, s.client()).andExpect(status().isForbidden());
        getAs("/api/projects/" + s.projectId() + "/tasks/00000000-0000-0000-0000-000000000000/time", s.pm()).andExpect(status().isNotFound());
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Time logged")));
    }

    @Test
    void timeEntriesAreChecked() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        String development = taskId(s.pm(), s.projectId(), "Development");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());
        assign(s.pm(), s.projectId(), development, s.dev2Id(), 10).andExpect(status().isOk());
        startProject(s.pm(), s.projectId());
        String url = "/api/projects/" + s.projectId() + "/tasks/" + design + "/time";

        postJson(url, entry(0, "0", null), s.dev()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.hours").exists());
        postJson(url, entry(0, "-3", null), s.dev()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.hours").exists());
        postJson(url, entry(0, "25", null), s.dev()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.hours").exists());
        postJson(url, "{\"hours\":2}", s.dev()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.date").exists());
        postJson(url, "{\"date\":\"" + day(0) + "\"}", s.dev()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.hours").exists());
        postJson(url, entry(0, "1", "x".repeat(501)), s.dev()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.note").exists());
        postJson(url, "{\"date\":\"" + day(2) + "\",\"hours\":2}", s.dev()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DATE_IN_FUTURE"));
        postJson(url, "{\"date\":\"tomorrow\",\"hours\":2}", s.dev()).andExpect(status().isBadRequest());

        // nobody logs time on a task that is not theirs, not even the manager
        postJson(url, entry(0, "1", null), s.dev2()).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_YOUR_TASK"));
        postJson(url, entry(0, "1", null), s.pm()).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_YOUR_TASK"));
        postJson(url, entry(0, "1", null), adminToken()).andExpect(status().isForbidden());
        postJson(url, entry(0, "1", null), s.client()).andExpect(status().isForbidden());
        postJson(url, entry(0, "1", null), "bad-token").andExpect(status().isUnauthorized());

        // the manager logs time on a task assigned to them
        String kickoff = taskId(s.pm(), s.projectId(), "Kick-off");
        postJson("/api/projects/" + s.projectId() + "/tasks/" + kickoff + "/time", entry(0, "3", "Workshop"), s.pm()).andExpect(status().isCreated());

        // no time on a finished task
        moveTask(s.pm(), s.projectId(), design, "DONE").andExpect(status().isOk());
        postJson(url, entry(0, "1", null), s.dev()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TASK_DONE"));
        getAs("/api/projects/" + s.projectId() + "/time", s.pm()).andExpect(jsonPath("$.loggedHours").value(3.0));
    }
}
