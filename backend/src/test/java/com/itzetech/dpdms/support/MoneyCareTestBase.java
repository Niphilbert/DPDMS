package com.itzetech.dpdms.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Helpers for the stage 3 tests: invoices, renewals, tickets and reports. */
public abstract class MoneyCareTestBase extends DeliveryTestBase {

    protected static String line(String description, String quantity, long unitPrice) {
        return "{\"description\":\"" + description + "\",\"quantity\":" + quantity + ",\"unitPrice\":" + unitPrice + "}";
    }

    /** Body for creating or editing an invoice. {@code tax} may be null. */
    protected static String invoiceBody(String projectId, String companyId, String issue, String due, String tax, String... lines) {
        StringBuilder sb = new StringBuilder("{");
        if (projectId != null) {
            sb.append("\"projectId\":\"").append(projectId).append("\",");
        }
        if (companyId != null) {
            sb.append("\"companyId\":\"").append(companyId).append("\",");
        }
        if (tax != null) {
            sb.append("\"taxRate\":").append(tax).append(',');
        }
        sb.append("\"issueDate\":\"").append(issue).append("\",\"dueDate\":\"").append(due).append("\",\"lines\":[")
                .append(String.join(",", lines)).append("]}");
        return sb.toString();
    }

    protected String companyIdOf(String token, String projectId) throws Exception {
        return read(getAs("/api/projects/" + projectId, token).andExpect(status().isOk()), "$.companyId");
    }

    /** Creates a draft invoice for the project with one line of the given amount; returns the invoice id. */
    protected String draftInvoice(String pm, String projectId, String issue, String due, long amount) throws Exception {
        return read(postJson("/api/invoices", invoiceBody(projectId, null, issue, due, null, line("Work", "1", amount)), pm)
                .andExpect(status().isCreated()), "$.id");
    }

    protected String sentInvoice(String pm, String projectId, String issue, String due, long amount) throws Exception {
        String id = draftInvoice(pm, projectId, issue, due, amount);
        postJson("/api/invoices/" + id + "/send", "{}", pm).andExpect(status().isOk());
        return id;
    }

    protected ResultActions pay(String pm, String invoiceId, long amount, String date) throws Exception {
        return postJson("/api/invoices/" + invoiceId + "/payments",
                "{\"amount\":" + amount + ",\"paidOn\":\"" + date + "\",\"method\":\"MOBILE_MONEY\",\"reference\":\"MoMo-1\"}", pm);
    }

    protected ResultActions putJson(String url, String json, String token) throws Exception {
        return mvc.perform(put(url).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json)
                .header("Authorization", "Bearer " + token));
    }

    protected String renewalBody(String projectId, String type, String label, String expiresOn, long price) {
        return "{\"projectId\":\"" + projectId + "\",\"type\":\"" + type + "\",\"label\":\"" + label + "\",\"provider\":\"Rwanda Host\","
                + "\"expiresOn\":\"" + expiresOn + "\",\"price\":" + price + ",\"note\":\"internal\"}";
    }

    protected String newRenewal(String pm, String projectId, String type, String label, String expiresOn, long price) throws Exception {
        return read(postJson("/api/renewals", renewalBody(projectId, type, label, expiresOn, price), pm).andExpect(status().isCreated()), "$.id");
    }

    /** A client raises a ticket; returns its id. */
    protected String newTicket(String client, String priority, String projectId) throws Exception {
        String body = "{\"subject\":\"Website is down\",\"description\":\"Patients cannot open the site.\",\"priority\":\"" + priority + "\""
                + (projectId == null ? "" : ",\"projectId\":\"" + projectId + "\"") + "}";
        return read(postJson("/api/tickets", body, client).andExpect(status().isCreated()), "$.id");
    }

    protected String commentBody(String text, boolean internal) {
        return "{\"body\":\"" + text + "\",\"internal\":" + internal + "}";
    }

    protected List<String> notificationMessages(String token) throws Exception {
        String body = getAs("/api/notifications", token).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].message");
    }

    protected long countNotifications(String token, String containing) throws Exception {
        return notificationMessages(token).stream().filter(m -> m.contains(containing)).count();
    }
}
