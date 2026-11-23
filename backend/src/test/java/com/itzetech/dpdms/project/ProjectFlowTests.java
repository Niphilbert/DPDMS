package com.itzetech.dpdms.project;

import com.itzetech.dpdms.support.DeliveryTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectFlowTests extends DeliveryTestBase {

    @Test
    void managerAssignsAndReassignsTasksAndPeopleAreTold() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");

        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(s.devId())).andExpect(jsonPath("$.dueDate").value(day(10)))
                .andExpect(jsonPath("$.status").value("TODO"));
        getAs("/api/notifications", s.dev()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("assigned you \"Design\""))));

        // moving it to another developer, with a note: the new one is told, the old one is told they are off it
        postJson("/api/projects/" + s.projectId() + "/tasks/" + design + "/assign",
                json("assigneeId", s.dev2Id(), "dueDate", day(12), "note", "Use the new brand colours"), s.pm())
                .andExpect(status().isOk()).andExpect(jsonPath("$.assigneeId").value(s.dev2Id())).andExpect(jsonPath("$.dueDate").value(day(12)));
        getAs("/api/notifications", s.dev2()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("Note: Use the new brand colours"))));
        getAs("/api/notifications", s.dev()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("has been reassigned to"))));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Task reassigned")))
                .andExpect(jsonPath("$[*].action", hasItem("Task assigned")));

        // the first developer no longer has anything on this project
        getAs("/api/projects/" + s.projectId(), s.dev()).andExpect(status().isNotFound());
        getAs("/api/projects/" + s.projectId(), s.dev2()).andExpect(status().isOk());
    }

    @Test
    void assigningIsChecked() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        String url = "/api/projects/" + s.projectId() + "/tasks/" + design + "/assign";

        postJson(url, json("assigneeId", s.clientId(), "dueDate", day(5)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_ASSIGNABLE"));
        postJson(url, json("assigneeId", "00000000-0000-0000-0000-000000000000", "dueDate", day(5)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_ASSIGNABLE"));
        postJson(url, json("assigneeId", s.devId(), "dueDate", day(-3)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DUE_DATE_PAST"));
        postJson(url, "{}", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.assigneeId").exists()).andExpect(jsonPath("$.fields.dueDate").exists());
        postJson("/api/projects/" + s.projectId() + "/tasks/00000000-0000-0000-0000-000000000000/assign",
                json("assigneeId", s.devId(), "dueDate", day(5)), s.pm()).andExpect(status().isNotFound());

        // only management assigns work
        assign(s.dev(), s.projectId(), design, s.devId(), 5).andExpect(status().isForbidden());
        assign(s.client(), s.projectId(), design, s.devId(), 5).andExpect(status().isForbidden());
        assign(newPm(), s.projectId(), design, s.devId(), 5).andExpect(status().isOk());
        assign(adminToken(), s.projectId(), design, s.dev2Id(), 5).andExpect(status().isOk());
        postJson("/api/admin/projects/" + s.projectId() + "/access", json("userId", s.devId()), adminToken()).andExpect(status().isOk());
        assign(s.dev(), s.projectId(), design, s.devId(), 5).andExpect(status().isForbidden());
    }

    @Test
    void theProjectManagerStartsTheProject() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());

        // nothing moves before the project starts
        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PROJECT_NOT_STARTED"));

        postJson("/api/projects/" + s.projectId() + "/start", "{}", s.dev()).andExpect(status().isForbidden());
        postJson("/api/projects/" + s.projectId() + "/start", "{}", s.client()).andExpect(status().isForbidden());
        postJson("/api/projects/" + s.projectId() + "/start", "{}", s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        getAs("/api/notifications", s.client()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("Work has started on your project"))));
        getAs("/api/notifications", s.dev()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("has started. Your tasks are ready."))));
        getAs("/api/projects/" + s.projectId() + "/updates", s.client()).andExpect(jsonPath("$[*].message", hasItem("Work has started on your project.")));
        postJson("/api/projects/" + s.projectId() + "/start", "{}", s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_STARTED"));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Project started")));
    }

    @Test
    void developersMoveOnlyTheirOwnTasksAndOnlyAlongAllowedSteps() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());
        startProject(s.pm(), s.projectId());

        // another developer cannot even see the project, and with access still cannot touch the task
        moveTask(s.dev2(), s.projectId(), design, "DOING").andExpect(status().isNotFound());
        postJson("/api/admin/projects/" + s.projectId() + "/access", json("userId", s.dev2Id()), adminToken()).andExpect(status().isOk());
        moveTask(s.dev2(), s.projectId(), design, "DOING").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_YOUR_TASK"));

        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DOING"));
        moveTask(s.dev(), s.projectId(), design, "DONE").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("MANAGER_ONLY"));
        moveTask(s.dev(), s.projectId(), design, "REVIEW").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SUBMISSION_REQUIRED"));
        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        moveTask(s.dev(), s.projectId(), design, "TODO").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("TODO"));
        moveTask(s.dev(), s.projectId(), design, "TODO").andExpect(status().isConflict());
        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isOk());
        moveTask(s.dev(), s.projectId(), design, "NONSENSE").andExpect(status().isBadRequest());

        // a client never moves tasks; management can move any task to any step except review
        moveTask(s.client(), s.projectId(), design, "DONE").andExpect(status().isForbidden());
        moveTask(s.pm(), s.projectId(), design, "TODO").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("TODO"));
        moveTask(s.pm(), s.projectId(), design, "TODO").andExpect(status().isConflict());
        moveTask(s.pm(), s.projectId(), design, "REVIEW").andExpect(status().isConflict());
        moveTask(s.pm(), s.projectId(), design, "DONE").andExpect(status().isOk());
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Task moved")));
    }

    @Test
    void workIsSubmittedReviewedSentBackAndCompleted() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        String base = "/api/projects/" + s.projectId() + "/tasks/" + design;
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());
        startProject(s.pm(), s.projectId());

        submitTask(s.dev(), s.projectId(), design).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION")).andExpect(jsonPath("$.message", containsString("Start the task")));
        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isOk());

        postJson(base + "/submit", "{\"file\":\"\",\"note\":\"x\"}", s.dev()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.file").exists());
        postJson(base + "/submit", json("file", "x.zip"), s.client()).andExpect(status().isForbidden());
        postJson("/api/admin/projects/" + s.projectId() + "/access", json("userId", s.dev2Id()), adminToken()).andExpect(status().isOk());
        submitTask(s.dev2(), s.projectId(), design).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_YOUR_TASK"));

        submitTask(s.dev(), s.projectId(), design).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW")).andExpect(jsonPath("$.submissionFile").value("https://files.example/work.zip"))
                .andExpect(jsonPath("$.submissionNote").value("Ready for review")).andExpect(jsonPath("$.submittedAt").exists());
        getAs("/api/notifications", s.pm()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("submitted \"Design\" for review"))));
        submitTask(s.dev(), s.projectId(), design).andExpect(status().isConflict());

        // only the manager reviews
        postJson(base + "/send-back", json("note", "Nope"), s.dev()).andExpect(status().isForbidden());
        postJson(base + "/complete", "{}", s.dev()).andExpect(status().isForbidden());
        postJson(base + "/complete", "{}", s.client()).andExpect(status().isForbidden());

        // send back needs comments, and tells the developer
        postJson(base + "/send-back", "{\"note\":\"  \"}", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.note").exists());
        postJson(base + "/send-back", json("note", "Mobile layout is missing"), s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOING")).andExpect(jsonPath("$.sentBackNote").value("Mobile layout is missing"));
        getAs("/api/notifications", s.dev()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("sent \"Design\" back with comments: Mobile layout is missing"))));
        postJson(base + "/complete", "{}", s.pm()).andExpect(status().isConflict());
        postJson(base + "/send-back", json("note", "Again"), s.pm()).andExpect(status().isConflict());

        // handing in again clears the old comments; the manager marks it done
        submitTask(s.dev(), s.projectId(), design).andExpect(status().isOk()).andExpect(jsonPath("$.sentBackNote").doesNotExist());
        postJson(base + "/complete", "{}", s.pm()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));
        getAs("/api/notifications", s.dev()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("\"Design\" in \"Booking website\" was marked done"))));

        // 20 of 104 planned hours are finished: 19 %
        getAs("/api/projects/" + s.projectId(), s.pm()).andExpect(jsonPath("$.progress").value(19))
                .andExpect(jsonPath("$.phases[0].progress").value(0)).andExpect(jsonPath("$.phases[1].progress").value(100))
                .andExpect(jsonPath("$.phases[1].taskCount").value(1));
        getAs("/api/projects/" + s.projectId() + "/updates", s.client()).andExpect(jsonPath("$[*].message", hasItem("Finished: Design")));

        // a finished task is closed for good
        postJson(base + "/complete", "{}", s.pm()).andExpect(status().isConflict());
        assign(s.pm(), s.projectId(), design, s.dev2Id(), 4).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TASK_DONE"));
        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isConflict());
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Task submitted for review")))
                .andExpect(jsonPath("$[*].action", hasItem("Task sent back"))).andExpect(jsonPath("$[*].action", hasItem("Task marked done")));
    }

    @Test
    void progressFollowsFinishedHoursAndTheProjectLaunchesWhenEverythingIsDone() throws Exception {
        Scenario s = scenario();
        String kickoff = taskId(s.pm(), s.projectId(), "Kick-off");
        String design = taskId(s.pm(), s.projectId(), "Design");
        String development = taskId(s.pm(), s.projectId(), "Development");
        String testing = taskId(s.pm(), s.projectId(), "Testing and launch");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());
        assign(s.pm(), s.projectId(), development, s.devId(), 20).andExpect(status().isOk());
        assign(s.pm(), s.projectId(), testing, s.dev2Id(), 30).andExpect(status().isOk());
        startProject(s.pm(), s.projectId());

        // the project manager does the kick-off themselves: 8 of 104 hours is 8 %
        moveTask(s.pm(), s.projectId(), kickoff, "DOING").andExpect(status().isOk());
        submitTask(s.pm(), s.projectId(), kickoff).andExpect(status().isOk());
        postJson("/api/projects/" + s.projectId() + "/tasks/" + kickoff + "/complete", "{}", s.pm()).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(8, progress(s.pm(), s.projectId()));
        org.junit.jupiter.api.Assertions.assertEquals("IN_PROGRESS", projectStatus(s.client(), s.projectId()));

        for (String[] t : new String[][]{{s.dev(), design}, {s.dev(), development}, {s.dev2(), testing}}) {
            moveTask(t[0], s.projectId(), t[1], "DOING").andExpect(status().isOk());
            submitTask(t[0], s.projectId(), t[1]).andExpect(status().isOk());
        }
        // everything is handed in, nothing is finished yet
        org.junit.jupiter.api.Assertions.assertEquals("IN_REVIEW", projectStatus(s.pm(), s.projectId()));

        postJson("/api/projects/" + s.projectId() + "/tasks/" + design + "/complete", "{}", s.pm()).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(27, progress(s.pm(), s.projectId()));
        postJson("/api/projects/" + s.projectId() + "/tasks/" + development + "/complete", "{}", s.pm()).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(85, progress(s.client(), s.projectId()));
        org.junit.jupiter.api.Assertions.assertEquals("IN_REVIEW", projectStatus(s.pm(), s.projectId()));

        // a handed-in task that is sent back puts the project back in progress
        postJson("/api/projects/" + s.projectId() + "/tasks/" + testing + "/send-back", json("note", "More tests"), s.pm()).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals("IN_PROGRESS", projectStatus(s.pm(), s.projectId()));
        submitTask(s.dev2(), s.projectId(), testing).andExpect(status().isOk());
        postJson("/api/projects/" + s.projectId() + "/tasks/" + testing + "/complete", "{}", s.pm()).andExpect(status().isOk());

        getAs("/api/projects/" + s.projectId(), s.client()).andExpect(jsonPath("$.progress").value(100))
                .andExpect(jsonPath("$.status").value("LAUNCHED")).andExpect(jsonPath("$.phases[3].progress").value(100));
        getAs("/api/notifications", s.client()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("has launched"))));
        getAs("/api/notifications", s.pm()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("is complete and launched"))));
        getAs("/api/projects/" + s.projectId() + "/updates", s.client()).andExpect(jsonPath("$[*].message", hasItem("All work is finished: your project has launched.")));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Project launched")));

        // a launched project takes no new tasks
        postJson("/api/projects/" + s.projectId() + "/tasks",
                "{\"title\":\"Late idea\",\"phase\":2,\"plannedHours\":5,\"dueDate\":\"" + day(10) + "\"}", s.pm())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PROJECT_LAUNCHED"));
    }

    @Test
    void managerAddsTasks() throws Exception {
        Scenario s = scenario();
        String url = "/api/projects/" + s.projectId() + "/tasks";
        String body = "{\"title\":\"Payment integration\",\"phase\":2,\"plannedHours\":30,\"dueDate\":\"" + day(25) + "\",\"assigneeId\":\"" + s.devId() + "\"}";

        postJson(url, body, s.pm()).andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Payment integration"))
                .andExpect(jsonPath("$.phase").value(2)).andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assigneeId").value(s.devId())).andExpect(jsonPath("$.plannedHours").value(30));
        getAs("/api/projects/" + s.projectId(), s.pm()).andExpect(jsonPath("$.tasks", hasSize(5))).andExpect(jsonPath("$.plannedHours").value(134))
                .andExpect(jsonPath("$.phases[2].taskCount").value(2));
        getAs("/api/notifications", s.dev()).andExpect(jsonPath("$.items[*].message", hasItem(containsString("assigned you \"Payment integration\""))));
        getAs("/api/projects/" + s.projectId(), s.dev()).andExpect(status().isOk());

        // a task without a developer yet is fine
        postJson(url, "{\"title\":\"Training\",\"phase\":3,\"plannedHours\":4,\"dueDate\":\"" + day(30) + "\"}", s.pm())
                .andExpect(status().isCreated()).andExpect(jsonPath("$.assigneeId").doesNotExist());

        postJson(url, "{\"title\":\"\",\"phase\":7,\"plannedHours\":0,\"dueDate\":null}", s.pm()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists()).andExpect(jsonPath("$.fields.phase").exists())
                .andExpect(jsonPath("$.fields.plannedHours").exists()).andExpect(jsonPath("$.fields.dueDate").exists());
        postJson(url, "{\"title\":\"Old\",\"phase\":1,\"plannedHours\":3,\"dueDate\":\"" + day(-1) + "\"}", s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DUE_DATE_PAST"));
        postJson(url, body.replace(s.devId(), s.clientId()), s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_ASSIGNABLE"));
        postJson(url, body, s.dev()).andExpect(status().isForbidden());
        postJson(url, body, s.client()).andExpect(status().isForbidden());
        postJson(url, body, s.dev2()).andExpect(status().isForbidden());
    }

    @Test
    void peopleSeeTheirOwnTasks() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());

        getAs("/api/tasks/mine", s.dev()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].task.title").value("Design")).andExpect(jsonPath("$[0].projectId").value(s.projectId()))
                .andExpect(jsonPath("$[0].projectName").value("Booking website")).andExpect(jsonPath("$[0].projectStatus").value("PLANNING"))
                .andExpect(jsonPath("$[0].task.budget").doesNotExist());
        getAs("/api/tasks/mine", s.pm()).andExpect(jsonPath("$[*].task.title", hasItem("Kick-off")));
        getAs("/api/tasks/mine", s.dev2()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/tasks/mine", s.client()).andExpect(status().isForbidden());
        getAs("/api/tasks/mine", null).andExpect(status().isUnauthorized());
    }

    @Test
    void lateTasksAreFlaggedUntilTheyAreDone() throws Exception {
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        jdbc.update("update project_task set due_date = ? where id = ?::uuid", java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(2)), design);

        getAs("/api/projects/" + s.projectId(), s.pm()).andExpect(jsonPath("$.lateTasks").value(1))
                .andExpect(jsonPath("$.tasks[?(@.title=='Design')].late").value(true));
        getAs("/api/projects/" + s.projectId(), s.client()).andExpect(jsonPath("$.lateTasks").value(1));
        getAs("/api/projects", s.pm()).andExpect(jsonPath("$[?(@.id=='" + s.projectId() + "')].lateTasks").value(1));

        moveTask(s.pm(), s.projectId(), design, "DONE").andExpect(status().isConflict()); // project not started yet
        startProject(s.pm(), s.projectId());
        moveTask(s.pm(), s.projectId(), design, "DONE").andExpect(status().isOk());
        getAs("/api/projects/" + s.projectId(), s.pm()).andExpect(jsonPath("$.lateTasks").value(0));
    }
}
