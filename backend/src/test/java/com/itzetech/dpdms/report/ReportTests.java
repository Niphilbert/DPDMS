package com.itzetech.dpdms.report;

import com.itzetech.dpdms.support.MoneyCareTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportTests extends MoneyCareTestBase {

    private String summary(String query) throws Exception {
        return getAs("/api/reports/summary" + query, adminToken()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static double num(String body, String path) {
        Object found = JsonPath.read(body, path);
        return ((Number) (found instanceof List<?> list ? list.get(0) : found)).doubleValue();
    }

    @Test
    void revenueNumbersMatchASmallSeededScenario() throws Exception {
        Scenario s = scenario();
        String june = "2019-07";
        // A: 1,000,000 with 400,000 paid, overdue.  B: 500,000 unpaid, overdue.  C: a draft.  D: sent then voided.  E: 200,000 paid in full.
        String a = sentInvoice(s.pm(), s.projectId(), june + "-05", june + "-20", 1_000_000);
        pay(s.pm(), a, 400_000, day(0)).andExpect(status().isCreated());
        sentInvoice(s.pm(), s.projectId(), june + "-10", "2019-08-10", 500_000);
        draftInvoice(s.pm(), s.projectId(), june + "-11", june + "-30", 300_000);
        String d = sentInvoice(s.pm(), s.projectId(), june + "-12", june + "-30", 250_000);
        postJson("/api/invoices/" + d + "/void", "{}", s.pm()).andExpect(status().isOk());
        String e = sentInvoice(s.pm(), s.projectId(), june + "-15", june + "-30", 200_000);
        pay(s.pm(), e, 200_000, day(0)).andExpect(status().isCreated());

        for (String token : new String[]{s.pm(), adminToken()}) {
            getAs("/api/reports/summary?from=2019-07-01&to=2019-07-31", token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.from").value("2019-07-01")).andExpect(jsonPath("$.to").value("2019-07-31")).andExpect(jsonPath("$.hourlyRate").value(25_000))
                    .andExpect(jsonPath("$.revenue.invoiced").value(1_700_000)).andExpect(jsonPath("$.revenue.invoiceCount").value(3))
                    .andExpect(jsonPath("$.revenue.paid").value(600_000)).andExpect(jsonPath("$.revenue.outstanding").value(1_100_000))
                    .andExpect(jsonPath("$.revenue.overdue").value(1_100_000)).andExpect(jsonPath("$.revenue.overdueCount").value(2))
                    .andExpect(jsonPath("$.revenue.byClient", hasSize(1))).andExpect(jsonPath("$.revenue.byClient[0].invoiced").value(1_700_000))
                    .andExpect(jsonPath("$.revenue.byClient[0].paid").value(600_000)).andExpect(jsonPath("$.generatedAt").isNotEmpty());
        }
        // only part of the month, and a month with nothing
        getAs("/api/reports/summary?from=2019-07-01&to=2019-07-10", s.pm()).andExpect(jsonPath("$.revenue.invoiced").value(1_500_000)).andExpect(jsonPath("$.revenue.invoiceCount").value(2));
        getAs("/api/reports/summary?from=2019-08-01&to=2019-08-31", s.pm()).andExpect(jsonPath("$.revenue.invoiced").value(0)).andExpect(jsonPath("$.revenue.byClient", hasSize(0)));
        getAs("/api/reports/summary?from=2019-08-01&to=2019-07-31", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RANGE"));
        getAs("/api/reports/summary?from=bad", s.pm()).andExpect(status().isBadRequest());
    }

    @Test
    void revenueByMonthCoversTheLastTwelveMonths() throws Exception {
        Scenario s = scenario();
        String before = getAs("/api/reports/revenue-by-month", s.pm()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(12))).andReturn().getResponse().getContentAsString();
        String id = sentInvoice(s.pm(), s.projectId(), day(0), day(10), 123_000);
        pay(s.pm(), id, 23_000, day(0)).andExpect(status().isCreated());
        draftInvoice(s.pm(), s.projectId(), day(0), day(10), 999_000);
        String after = getAs("/api/reports/revenue-by-month", s.pm()).andReturn().getResponse().getContentAsString();

        String thisMonth = java.time.YearMonth.now().toString();
        assertThat((String) JsonPath.read(after, "$[11].month")).isEqualTo(thisMonth);
        assertThat((String) JsonPath.read(after, "$[0].month")).isEqualTo(java.time.YearMonth.now().minusMonths(11).toString());
        assertThat(num(after, "$[11].invoiced") - num(before, "$[11].invoiced")).isEqualTo(123_000.0);
        assertThat(num(after, "$[11].paid") - num(before, "$[11].paid")).isEqualTo(23_000.0);
        assertThat(num(after, "$[0].invoiced")).isEqualTo(num(before, "$[0].invoiced"));

        // a reversed payment stops counting
        String payment = read(getAs("/api/invoices/" + id, s.pm()), "$.payments[0].id");
        postJson("/api/invoices/" + id + "/payments/" + payment + "/reverse", "{\"reason\":\"Mistake\"}", s.pm()).andExpect(status().isOk());
        String reversed = getAs("/api/reports/revenue-by-month", s.pm()).andReturn().getResponse().getContentAsString();
        assertThat(num(reversed, "$[11].paid")).isEqualTo(num(before, "$[11].paid"));

        getAs("/api/reports/revenue-by-month?months=3", s.pm()).andExpect(jsonPath("$", hasSize(3))).andExpect(jsonPath("$[2].month").value(thisMonth));
        getAs("/api/reports/revenue-by-month?months=0", s.pm()).andExpect(status().isBadRequest());
        getAs("/api/reports/revenue-by-month?months=37", s.pm()).andExpect(status().isBadRequest());
    }

    @Test
    void deliveryTicketRenewalAndHourNumbersMatchTheSeededScenario() throws Exception {
        String projectsBefore = summary("");
        Scenario s = scenario();
        String design = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());
        startProject(s.pm(), s.projectId());
        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isOk());
        String timeUrl = "/api/projects/" + s.projectId() + "/tasks/" + design + "/time";
        postJson(timeUrl, "{\"date\":\"" + day(0) + "\",\"hours\":3}", s.dev()).andExpect(status().isCreated());
        postJson(timeUrl, "{\"date\":\"" + day(-1) + "\",\"hours\":1.5}", s.dev()).andExpect(status().isCreated());

        newRenewal(s.pm(), s.projectId(), "DOMAIN", "rep-expiring.example", day(10), 25_000);
        newRenewal(s.pm(), s.projectId(), "DOMAIN", "rep-expired.example", day(-3), 25_000);
        newRenewal(s.pm(), s.projectId(), "SSL", "rep-active.example", day(90), 60_000);

        String urgent = newTicket(s.client(), "URGENT", null);
        newTicket(s.client(), "LOW", null);
        postJson("/api/tickets/" + urgent + "/status", json("status", "RESOLVED"), s.pm()).andExpect(status().isOk());

        String after = summary("");
        assertThat(num(after, "$.projects.total") - num(projectsBefore, "$.projects.total")).isEqualTo(1.0);
        assertThat(num(after, "$.projects.byStatus.IN_PROGRESS") - num(projectsBefore, "$.projects.byStatus.IN_PROGRESS")).isEqualTo(1.0);
        assertThat(num(after, "$.projects.byStatus.LAUNCHED")).isEqualTo(num(projectsBefore, "$.projects.byStatus.LAUNCHED"));
        assertThat((Object) JsonPath.read(after, "$.projects.averageProgress")).isNotNull();

        assertThat(num(after, "$.tickets.total") - num(projectsBefore, "$.tickets.total")).isEqualTo(2.0);
        assertThat(num(after, "$.tickets.open") - num(projectsBefore, "$.tickets.open")).isEqualTo(1.0);
        assertThat(num(after, "$.tickets.resolved") - num(projectsBefore, "$.tickets.resolved")).isEqualTo(1.0);
        assertThat(num(after, "$.tickets.byPriority[?(@.priority=='URGENT')].total") - num(projectsBefore, "$.tickets.byPriority[?(@.priority=='URGENT')].total")).isEqualTo(1.0);
        assertThat(num(after, "$.tickets.byPriority[?(@.priority=='LOW')].open") - num(projectsBefore, "$.tickets.byPriority[?(@.priority=='LOW')].open")).isEqualTo(1.0);
        List<String> priorities = JsonPath.read(after, "$.tickets.byPriority[*].priority");
        assertThat(priorities).containsExactly("URGENT", "HIGH", "MEDIUM", "LOW");
        double within = num(after, "$.tickets.resolvedWithinSlaPercent");
        assertThat(within).isBetween(0.0, 100.0);
        assertThat(num(after, "$.tickets.averageResolutionHours")).isGreaterThanOrEqualTo(0.0);

        assertThat(num(after, "$.renewals.total") - num(projectsBefore, "$.renewals.total")).isEqualTo(3.0);
        assertThat(num(after, "$.renewals.expiring") - num(projectsBefore, "$.renewals.expiring")).isEqualTo(1.0);
        assertThat(num(after, "$.renewals.expired") - num(projectsBefore, "$.renewals.expired")).isEqualTo(1.0);
        assertThat(num(after, "$.renewals.byType[?(@.type=='DOMAIN')].expiring") - num(projectsBefore, "$.renewals.byType[?(@.type=='DOMAIN')].expiring")).isEqualTo(1.0);
        assertThat(num(after, "$.renewals.byType[?(@.type=='DOMAIN')].expired") - num(projectsBefore, "$.renewals.byType[?(@.type=='DOMAIN')].expired")).isEqualTo(1.0);
        assertThat(num(after, "$.renewals.byType[?(@.type=='SSL')].active") - num(projectsBefore, "$.renewals.byType[?(@.type=='SSL')].active")).isEqualTo(1.0);
        List<String> types = JsonPath.read(after, "$.renewals.byType[*].type");
        assertThat(types).containsExactly("DOMAIN", "HOSTING", "SSL", "MAINTENANCE");

        // hours by developer: this developer logged 4.5 hours; the range limits by work date
        assertThat(num(after, "$.hours.byDeveloper[?(@.userId=='" + s.devId() + "')].hours")).isEqualTo(4.5);
        assertThat(num(after, "$.hours.total") - num(projectsBefore, "$.hours.total")).isCloseTo(4.5, org.assertj.core.data.Offset.offset(0.001));
        String justToday = summary("?from=" + day(0) + "&to=" + day(0));
        assertThat(num(justToday, "$.hours.byDeveloper[?(@.userId=='" + s.devId() + "')].hours")).isEqualTo(3.0);

        // project profit: 4.5 h at 25,000 = 112,500 so far; the forecast uses the planned 104 h = 2,600,000 against a 1,500,000 budget
        String row = "$.projectProfit[?(@.projectId=='" + s.projectId() + "')]";
        assertThat(num(after, row + ".budget")).isEqualTo(1_500_000.0);
        assertThat(num(after, row + ".loggedHours")).isEqualTo(4.5);
        assertThat(num(after, row + ".costSoFar")).isEqualTo(112_500.0);
        assertThat(num(after, row + ".plannedHours")).isEqualTo(104.0);
        assertThat(num(after, row + ".forecastCost")).isEqualTo(2_600_000.0);
        assertThat(num(after, row + ".forecastMarginPercent")).isEqualTo(-73.0);
        assertThat((String) ((List<?>) JsonPath.read(after, row + ".status")).get(0)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void reportsAreForManagementOnly() throws Exception {
        Scenario s = scenario();
        for (String url : new String[]{"/api/reports/summary", "/api/reports/summary.csv", "/api/reports/revenue-by-month"}) {
            getAs(url, s.dev()).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
            getAs(url, s.client()).andExpect(status().isForbidden());
            getAs(url, null).andExpect(status().isUnauthorized());
            getAs(url, s.pm()).andExpect(status().isOk());
            getAs(url, adminToken()).andExpect(status().isOk());
        }
        getAs("/api/companies", s.client()).andExpect(status().isForbidden());
        getAs("/api/companies", s.dev()).andExpect(status().isForbidden());
        getAs("/api/companies", s.pm()).andExpect(status().isOk()).andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.hasItem(
                read(getAs("/api/auth/me", s.client()), "$.companyName"))));
    }

    @Test
    void theSummaryCanBeDownloadedAsCsv() throws Exception {
        Scenario s = scenario();
        sentInvoice(s.pm(), s.projectId(), "2018-03-05", "2018-03-20", 100_000);
        getAs("/api/reports/summary.csv?from=2018-03-01&to=2018-03-31", s.pm()).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("reports-summary.csv"))).andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Section,Item,Value")))
                .andExpect(content().string(containsString("Revenue,Invoiced,100000")))
                .andExpect(content().string(containsString("Tickets,URGENT total,")))
                .andExpect(content().string(containsString("Renewals,SSL expired,")))
                .andExpect(content().string(containsString("Hours,Total,")));
    }
}
