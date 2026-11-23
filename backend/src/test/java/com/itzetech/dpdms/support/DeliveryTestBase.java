package com.itzetech.dpdms.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Helpers for the requests, projects, approvals and time tests: set up people and walk a request through to a project. */
public abstract class DeliveryTestBase extends ApiTestBase {

    @Autowired
    protected JdbcTemplate jdbc;

    /** A request that has been quoted and accepted, with one person of every kind. */
    public record Scenario(String pm, String pmId, String dev, String devId, String dev2, String dev2Id,
                           String client, String clientId, String projectId, String requestId) {
    }

    protected static String day(int plusDays) {
        return LocalDate.now().plusDays(plusDays).toString();
    }

    protected ResultActions deleteAs(String url, String token) throws Exception {
        return mvc.perform(delete(url).header("Authorization", "Bearer " + token));
    }

    protected String meId(String token) throws Exception {
        return read(getAs("/api/auth/me", token), "$.id");
    }

    protected String newPm() throws Exception {
        return newStaffToken(unique("pm") + "@itzetech.example", "PROJECT_MANAGER");
    }

    protected String newDev() throws Exception {
        return newStaffToken(unique("dev") + "@itzetech.example", "DEVELOPER");
    }

    protected String newClient(String company) throws Exception {
        return newClientToken(unique("c") + "@client.example", company);
    }

    protected String requestBody(String title) {
        return "{\"title\":\"" + title + "\",\"description\":\"We need a booking website with online payments.\","
                + "\"budget\":\"RWF 3,000,000\",\"deadline\":\"" + day(60) + "\"}";
    }

    protected String sendRequest(String clientToken, String title) throws Exception {
        return read(postJson("/api/requests", requestBody(title), clientToken).andExpect(status().isCreated()), "$.id");
    }

    protected String quoteBody(long... amounts) {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < amounts.length; i++) {
            if (i > 0) {
                items.append(',');
            }
            items.append("{\"description\":\"Line ").append(i + 1).append("\",\"amount\":").append(amounts[i]).append('}');
        }
        return "{\"items\":[" + items + "],\"weeks\":8,\"validUntil\":\"" + day(14) + "\"}";
    }

    protected void sendQuote(String pmToken, String requestId, long... amounts) throws Exception {
        postJson("/api/requests/" + requestId + "/quote", quoteBody(amounts), pmToken).andExpect(status().isOk());
    }

    /** Client sends a request, the project manager quotes 1,500,000 RWF, the client accepts. Returns everyone involved. */
    protected Scenario scenario() throws Exception {
        String pm = newPm();
        String dev = newDev();
        String dev2 = newDev();
        String client = newClient("Acme " + unique("Hotels"));
        String requestId = sendRequest(client, "Booking website");
        sendQuote(pm, requestId, 1_000_000, 500_000);
        String projectId = read(postJson("/api/requests/" + requestId + "/accept", "{}", client).andExpect(status().isOk()), "$.projectId");
        return new Scenario(pm, meId(pm), dev, meId(dev), dev2, meId(dev2), client, meId(client), projectId, requestId);
    }

    protected String taskId(String token, String projectId, String title) throws Exception {
        String body = getAs("/api/projects/" + projectId, token).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.tasks[?(@.title=='" + title + "')].id");
        return ids.get(0);
    }

    protected ResultActions assign(String pm, String projectId, String taskId, String userId, int dueInDays) throws Exception {
        return postJson("/api/projects/" + projectId + "/tasks/" + taskId + "/assign",
                "{\"assigneeId\":\"" + userId + "\",\"dueDate\":\"" + day(dueInDays) + "\"}", pm);
    }

    protected ResultActions moveTask(String token, String projectId, String taskId, String newStatus) throws Exception {
        return postJson("/api/projects/" + projectId + "/tasks/" + taskId + "/status", json("status", newStatus), token);
    }

    protected ResultActions submitTask(String token, String projectId, String taskId) throws Exception {
        return postJson("/api/projects/" + projectId + "/tasks/" + taskId + "/submit",
                json("file", "https://files.example/work.zip", "note", "Ready for review"), token);
    }

    protected void startProject(String pm, String projectId) throws Exception {
        postJson("/api/projects/" + projectId + "/start", "{}", pm).andExpect(status().isOk());
    }

    /** The current status of a project, as the given person sees it. */
    protected String projectStatus(String token, String projectId) throws Exception {
        return read(getAs("/api/projects/" + projectId, token).andExpect(status().isOk()), "$.status");
    }

    protected int progress(String token, String projectId) throws Exception {
        return Integer.parseInt(read(getAs("/api/projects/" + projectId, token).andExpect(status().isOk()), "$.progress"));
    }
}
