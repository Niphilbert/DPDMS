package com.itzetech.dpdms.invoice;

import com.itzetech.dpdms.support.MoneyCareTestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvoiceTests extends MoneyCareTestBase {

    @Test
    void numbersRunPerYearAndTotalsAreWorkedOut() throws Exception {
        Scenario s = scenario();
        String body = invoiceBody(s.projectId(), null, "2041-03-01", "2041-03-15", "18", line("Design", "1", 500_000), line("Hours", "11.75", 25_000));
        postJson("/api/invoices", body, s.pm()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("INV-2041-0001")).andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.displayStatus").value("DRAFT")).andExpect(jsonPath("$.subtotal").value(793_750))
                .andExpect(jsonPath("$.taxAmount").value(142_875)).andExpect(jsonPath("$.total").value(936_625))
                .andExpect(jsonPath("$.balance").value(936_625)).andExpect(jsonPath("$.paidAmount").value(0))
                .andExpect(jsonPath("$.lines", hasSize(2))).andExpect(jsonPath("$.lines[1].lineTotal").value(293_750))
                .andExpect(jsonPath("$.payments", hasSize(0))).andExpect(jsonPath("$.projectName").value("Booking website"))
                .andExpect(jsonPath("$.createdByName").value("Test PROJECT_MANAGER"));
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2041-04-01", "2041-04-15", null, line("Half", "0.5", 333)), adminToken())
                .andExpect(status().isCreated()).andExpect(jsonPath("$.number").value("INV-2041-0002"))
                .andExpect(jsonPath("$.subtotal").value(167)).andExpect(jsonPath("$.taxAmount").value(0)).andExpect(jsonPath("$.taxRate").value(0));
        // another year starts again at 1
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2042-01-01", "2042-01-31", null, line("Work", "1", 1000)), s.pm())
                .andExpect(status().isCreated()).andExpect(jsonPath("$.number").value("INV-2042-0001"));
        // a company invoice without a project
        postJson("/api/invoices", invoiceBody(null, companyIdOf(s.pm(), s.projectId()), "2042-02-01", "2042-02-10", null, line("Domain renewal", "1", 25_000)), s.pm())
                .andExpect(status().isCreated()).andExpect(jsonPath("$.number").value("INV-2042-0002")).andExpect(jsonPath("$.projectId").value((Object) null));
    }

    @Test
    void twoPeopleCreatingInvoicesAtOnceNeverShareANumber() throws Exception {
        Scenario s = scenario();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> jobs = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                jobs.add(() -> read(postJson("/api/invoices", invoiceBody(s.projectId(), null, "2043-05-01", "2043-05-30", null, line("Work", "1", 1000)), s.pm())
                        .andExpect(status().isCreated()), "$.number"));
            }
            TreeSet<String> numbers = new TreeSet<>();
            for (Future<String> f : pool.invokeAll(jobs)) {
                numbers.add(f.get());
            }
            assertThat(numbers).hasSize(8);
            assertThat(numbers.first()).isEqualTo("INV-2043-0001");
            assertThat(numbers.last()).isEqualTo("INV-2043-0008");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void invoiceInputIsChecked() throws Exception {
        Scenario s = scenario();
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2044-03-10", "2044-03-01", null, line("Work", "1", 1000)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DUE_BEFORE_ISSUE"));
        postJson("/api/invoices", "{\"projectId\":\"" + s.projectId() + "\",\"issueDate\":\"2044-03-01\",\"dueDate\":\"2044-03-10\",\"lines\":[]}", s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(jsonPath("$.fields.lines").exists());
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2044-03-01", "2044-03-10", null, line("", "1", 1000)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields['lines[0].description']").exists());
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2044-03-01", "2044-03-10", null, line("x", "0", 1000)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields['lines[0].quantity']").exists());
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2044-03-01", "2044-03-10", null, line("x", "1", 0)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields['lines[0].unitPrice']").exists());
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2044-03-01", "2044-03-10", "101", line("x", "1", 10)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.taxRate").exists());
        postJson("/api/invoices", invoiceBody(null, null, "2044-03-01", "2044-03-10", null, line("x", "1", 10)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.companyId").exists());
        postJson("/api/invoices", invoiceBody(s.projectId(), "00000000-0000-0000-0000-000000000001", "2044-03-01", "2044-03-10", null, line("x", "1", 10)), s.pm())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.companyId").exists());
        postJson("/api/invoices", invoiceBody("00000000-0000-0000-0000-000000000001", null, "2044-03-01", "2044-03-10", null, line("x", "1", 10)), s.pm())
                .andExpect(status().isNotFound());
        // only management writes
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2044-03-01", "2044-03-10", null, line("x", "1", 10)), s.client())
                .andExpect(status().isForbidden());
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2044-03-01", "2044-03-10", null, line("x", "1", 10)), s.dev())
                .andExpect(status().isForbidden());
        postJson("/api/invoices", invoiceBody(s.projectId(), null, "2044-03-01", "2044-03-10", null, line("x", "1", 10)), "bad").andExpect(status().isUnauthorized());
    }

    @Test
    void draftsAreEditableAndInvisibleToTheClientUntilSent() throws Exception {
        Scenario s = scenario();
        String id = draftInvoice(s.pm(), s.projectId(), day(0), day(14), 1_000_000);
        getAs("/api/invoices/" + id, s.client()).andExpect(status().isNotFound());
        getAs("/api/invoices", s.client()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/projects/" + s.projectId() + "/invoices", s.client()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getAs("/api/projects/" + s.projectId() + "/invoices", s.pm()).andExpect(jsonPath("$", hasSize(1)));

        putJson("/api/invoices/" + id, invoiceBody(s.projectId(), null, day(0), day(30), "18", line("A", "2", 300_000), line("B", "1", 50_000)), s.pm())
                .andExpect(status().isOk()).andExpect(jsonPath("$.lines", hasSize(2))).andExpect(jsonPath("$.subtotal").value(650_000))
                .andExpect(jsonPath("$.total").value(767_000)).andExpect(jsonPath("$.dueDate").value(day(30)));
        putJson("/api/invoices/" + id, invoiceBody(s.projectId(), null, day(0), day(30), null, line("A", "1", 5)), s.client()).andExpect(status().isForbidden());
        pay(s.pm(), id, 1000, day(0)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVOICE_NOT_PAYABLE"));

        postJson("/api/invoices/" + id + "/send", "{}", s.client()).andExpect(status().isForbidden());
        long before = countNotifications(s.client(), "New invoice");
        postJson("/api/invoices/" + id + "/send", "{}", s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT")).andExpect(jsonPath("$.sentAt").isNotEmpty());
        assertThat(countNotifications(s.client(), "New invoice")).isEqualTo(before + 1);
        postJson("/api/invoices/" + id + "/send", "{}", s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVOICE_NOT_DRAFT"));
        putJson("/api/invoices/" + id, invoiceBody(s.projectId(), null, day(0), day(30), null, line("A", "1", 5)), s.pm())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVOICE_NOT_DRAFT"));

        getAs("/api/invoices/" + id, s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.voidReason").value((Object) null));
        getAs("/api/invoices", s.client()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].lines").doesNotExist())
                .andExpect(jsonPath("$[0].payments").doesNotExist());
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Invoice created")))
                .andExpect(jsonPath("$[*].action", hasItem("Invoice sent")));
    }

    @Test
    void paymentsAddUpReverseAndNeverExceedTheBalance() throws Exception {
        Scenario s = scenario();
        String id = sentInvoice(s.pm(), s.projectId(), day(0), day(14), 1_000_000);

        pay(s.pm(), id, 400_000, day(0)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.paidAmount").value(400_000)).andExpect(jsonPath("$.balance").value(600_000))
                .andExpect(jsonPath("$.payments", hasSize(1))).andExpect(jsonPath("$.payments[0].method").value("MOBILE_MONEY"))
                .andExpect(jsonPath("$.payments[0].reference").value("MoMo-1")).andExpect(jsonPath("$.payments[0].recordedByName").value("Test PROJECT_MANAGER"));
        pay(s.pm(), id, 600_001, day(0)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OVERPAYMENT"));
        getAs("/api/invoices/" + id, s.pm()).andExpect(jsonPath("$.paidAmount").value(400_000)).andExpect(jsonPath("$.payments", hasSize(1)));
        pay(s.pm(), id, 0, day(0)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.amount").exists());
        pay(s.pm(), id, 10, day(2)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DATE_IN_FUTURE"));
        postJson("/api/invoices/" + id + "/payments", "{\"amount\":10,\"paidOn\":\"" + day(0) + "\",\"method\":\"CHEQUE\"}", s.pm()).andExpect(status().isBadRequest());
        pay(s.client(), id, 10, day(0)).andExpect(status().isForbidden());
        pay(s.dev(), id, 10, day(0)).andExpect(status().isForbidden());

        long clientNotes = countNotifications(s.client(), "received your payment");
        String second = read(pay(s.pm(), id, 600_000, day(0)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.balance").value(0)), "$.payments[1].id");
        assertThat(countNotifications(s.client(), "received your payment")).isEqualTo(clientNotes + 1);
        pay(s.pm(), id, 1, day(0)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVOICE_NOT_PAYABLE"));

        // reversing keeps the row and recomputes the status
        postJson("/api/invoices/" + id + "/payments/" + second + "/reverse", "{}", s.pm()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.reason").exists());
        postJson("/api/invoices/" + id + "/payments/" + second + "/reverse", "{\"reason\":\"Cheque bounced\"}", s.client()).andExpect(status().isForbidden());
        postJson("/api/invoices/" + id + "/payments/" + second + "/reverse", "{\"reason\":\"Cheque bounced\"}", s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID")).andExpect(jsonPath("$.paidAmount").value(400_000))
                .andExpect(jsonPath("$.payments", hasSize(2))).andExpect(jsonPath("$.payments[1].reversed").value(true))
                .andExpect(jsonPath("$.payments[1].reverseReason").value("Cheque bounced"));
        postJson("/api/invoices/" + id + "/payments/" + second + "/reverse", "{\"reason\":\"again\"}", s.pm())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAYMENT_ALREADY_REVERSED"));
        postJson("/api/invoices/" + id + "/payments/00000000-0000-0000-0000-000000000001/reverse", "{\"reason\":\"x\"}", s.pm()).andExpect(status().isNotFound());

        // the client sees the payments, but not who reversed them or why
        getAs("/api/invoices/" + id, s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.payments", hasSize(2)))
                .andExpect(jsonPath("$.payments[1].reversed").value(true)).andExpect(jsonPath("$.payments[1].reverseReason").value((Object) null))
                .andExpect(jsonPath("$.payments[1].reversedByName").value((Object) null));

        // the reversed amount can be paid again
        pay(s.pm(), id, 600_000, day(-1)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PAID"));
        getAs("/api/admin/audit?limit=300", adminToken()).andExpect(jsonPath("$[*].action", hasItem("Payment recorded")))
                .andExpect(jsonPath("$[*].action", hasItem("Payment reversed")));
    }

    @Test
    void voidingFollowsTheRules() throws Exception {
        Scenario s = scenario();
        String id = sentInvoice(s.pm(), s.projectId(), day(0), day(14), 500_000);
        String paymentId = read(pay(s.pm(), id, 100_000, day(0)).andExpect(status().isCreated()), "$.payments[0].id");

        postJson("/api/invoices/" + id + "/void", "{\"reason\":\"Wrong client\"}", s.client()).andExpect(status().isForbidden());
        postJson("/api/invoices/" + id + "/void", "{}", s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVOICE_HAS_PAYMENTS"));
        postJson("/api/invoices/" + id + "/payments/" + paymentId + "/reverse", "{\"reason\":\"Paid twice\"}", s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
        long before = countNotifications(s.client(), "cancelled");
        postJson("/api/invoices/" + id + "/void", "{\"reason\":\"Wrong client\"}", s.pm()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOID")).andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.voidReason").value("Wrong client")).andExpect(jsonPath("$.voidedAt").isNotEmpty());
        assertThat(countNotifications(s.client(), "cancelled")).isEqualTo(before + 1);
        postJson("/api/invoices/" + id + "/void", "{}", s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
        pay(s.pm(), id, 1, day(0)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVOICE_NOT_PAYABLE"));
        // a sent invoice stays visible to the client as void, without the internal reason
        getAs("/api/invoices/" + id, s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VOID")).andExpect(jsonPath("$.voidReason").value((Object) null));

        // a draft can be voided with no body at all, and then stays hidden from the client
        String draft = draftInvoice(s.pm(), s.projectId(), day(0), day(14), 1000);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/invoices/" + draft + "/void")
                .header("Authorization", "Bearer " + s.pm())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VOID"));
        getAs("/api/invoices/" + draft, s.client()).andExpect(status().isNotFound());
        // voided invoices are not money owed
        getAs("/api/invoices/summary", s.client()).andExpect(jsonPath("$.invoiceCount").value(0)).andExpect(jsonPath("$.balanceDue").value(0));
    }

    @Test
    void overdueIsWorkedOutFromTheDueDateAndNeverStored() throws Exception {
        Scenario s = scenario();
        String late = sentInvoice(s.pm(), s.projectId(), day(-30), day(-10), 800_000);
        String fine = sentInvoice(s.pm(), s.projectId(), day(-5), day(9), 200_000);
        String dueToday = sentInvoice(s.pm(), s.projectId(), day(-5), day(0), 100_000);

        getAs("/api/invoices/" + late, s.client()).andExpect(jsonPath("$.status").value("SENT")).andExpect(jsonPath("$.displayStatus").value("OVERDUE"))
                .andExpect(jsonPath("$.overdue").value(true)).andExpect(jsonPath("$.daysOverdue").value(10));
        getAs("/api/invoices/" + fine, s.client()).andExpect(jsonPath("$.displayStatus").value("SENT")).andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.daysOverdue").value((Object) null));
        getAs("/api/invoices/" + dueToday, s.client()).andExpect(jsonPath("$.overdue").value(false));
        getAs("/api/invoices?status=OVERDUE&projectId=" + s.projectId(), s.pm()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(late));
        getAs("/api/invoices?status=SENT&projectId=" + s.projectId(), s.pm()).andExpect(jsonPath("$", hasSize(3)));
        getAs("/api/invoices?status=NOPE", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // part payment keeps it overdue; paying in full is not overdue any more
        pay(s.pm(), late, 300_000, day(0)).andExpect(jsonPath("$.status").value("PARTIALLY_PAID")).andExpect(jsonPath("$.displayStatus").value("OVERDUE"));
        getAs("/api/invoices/summary?projectId=" + s.projectId(), s.pm()).andExpect(jsonPath("$.overdueCount").value(1)).andExpect(jsonPath("$.overdueAmount").value(500_000));
        pay(s.pm(), late, 500_000, day(0)).andExpect(jsonPath("$.status").value("PAID")).andExpect(jsonPath("$.displayStatus").value("PAID")).andExpect(jsonPath("$.overdue").value(false));
        getAs("/api/invoices?status=OVERDUE&projectId=" + s.projectId(), s.pm()).andExpect(jsonPath("$", hasSize(0)));

        // reminders only for invoices waiting for payment
        postJson("/api/invoices/" + fine + "/remind", "{}", s.client()).andExpect(status().isForbidden());
        long before = countNotifications(s.client(), "Reminder");
        postJson("/api/invoices/" + fine + "/remind", "{}", s.pm()).andExpect(status().isOk());
        assertThat(countNotifications(s.client(), "Reminder")).isEqualTo(before + 1);
        postJson("/api/invoices/" + late + "/remind", "{}", s.pm()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test
    void clientsSeeOnlyTheirOwnCompanysSentInvoices() throws Exception {
        Scenario s = scenario();
        Scenario other = scenario();
        String mine = sentInvoice(s.pm(), s.projectId(), day(0), day(14), 1_180_000);
        String theirs = sentInvoice(s.pm(), other.projectId(), day(0), day(14), 70_000);
        draftInvoice(s.pm(), s.projectId(), day(0), day(14), 5_000);

        getAs("/api/invoices/" + theirs, s.client()).andExpect(status().isNotFound());
        getAs("/api/invoices/" + mine, other.client()).andExpect(status().isNotFound());
        getAs("/api/invoices", s.client()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(mine));
        getAs("/api/projects/" + other.projectId() + "/invoices", s.client()).andExpect(status().isNotFound());
        getAs("/api/projects/" + other.projectId() + "/money", s.client()).andExpect(status().isNotFound());
        // a client cannot widen the list to another company
        getAs("/api/invoices?companyId=" + companyIdOf(other.pm(), other.projectId()), s.client()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(mine));
        // clients cannot write or export
        getAs("/api/invoices/export.csv", s.client()).andExpect(status().isForbidden());
        getAs("/api/projects/" + s.projectId() + "/invoice-suggestion", s.client()).andExpect(status().isForbidden());

        pay(s.pm(), mine, 180_000, day(0)).andExpect(status().isCreated());
        getAs("/api/invoices/summary", s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.invoiceCount").value(1))
                .andExpect(jsonPath("$.invoiced").value(1_180_000)).andExpect(jsonPath("$.paid").value(180_000))
                .andExpect(jsonPath("$.balanceDue").value(1_000_000)).andExpect(jsonPath("$.openCount").value(1)).andExpect(jsonPath("$.overdueCount").value(0));
        getAs("/api/projects/" + s.projectId() + "/money", s.client()).andExpect(status().isOk()).andExpect(jsonPath("$.budget").value(1_500_000))
                .andExpect(jsonPath("$.invoiced").value(1_180_000)).andExpect(jsonPath("$.paid").value(180_000))
                .andExpect(jsonPath("$.balanceDue").value(1_000_000)).andExpect(jsonPath("$.unbilled").value(320_000));
        getAs("/api/projects/" + s.projectId() + "/money", s.pm()).andExpect(status().isOk()).andExpect(jsonPath("$.invoiced").value(1_180_000));
        // the existing project answer is unchanged
        getAs("/api/projects/" + s.projectId(), s.client()).andExpect(jsonPath("$.budget").value(1_500_000)).andExpect(jsonPath("$.invoiced").doesNotExist());
    }

    @Test
    void developersHaveNoAccessToMoneyAtAll() throws Exception {
        Scenario s = scenario();
        String taskId = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), taskId, s.devId(), 10).andExpect(status().isOk());
        String id = sentInvoice(s.pm(), s.projectId(), day(0), day(14), 100_000);

        getAs("/api/invoices", s.dev()).andExpect(status().isForbidden());
        getAs("/api/invoices/" + id, s.dev()).andExpect(status().isForbidden());
        getAs("/api/invoices/summary", s.dev()).andExpect(status().isForbidden());
        getAs("/api/invoices/export.csv", s.dev()).andExpect(status().isForbidden());
        getAs("/api/projects/" + s.projectId() + "/invoices", s.dev()).andExpect(status().isForbidden());
        getAs("/api/projects/" + s.projectId() + "/money", s.dev()).andExpect(status().isForbidden());
        getAs("/api/projects/" + s.projectId() + "/invoice-suggestion", s.dev()).andExpect(status().isForbidden());
        postJson("/api/invoices/" + id + "/remind", "{}", s.dev()).andExpect(status().isForbidden());
        // a developer who cannot see the project gets 404 rather than learning that it exists
        getAs("/api/projects/" + s.projectId() + "/money", s.dev2()).andExpect(status().isNotFound());
        getAs("/api/projects/" + s.projectId() + "/invoices", s.dev2()).andExpect(status().isNotFound());
        getAs("/api/invoices", null).andExpect(status().isUnauthorized());
    }

    @Test
    void invoiceSuggestionUsesLoggedHoursAndTheRemainingBudget() throws Exception {
        Scenario s = scenario();
        getAs("/api/projects/" + s.projectId() + "/invoice-suggestion", s.pm()).andExpect(status().isOk()).andExpect(jsonPath("$.hourlyRate").value(25_000))
                .andExpect(jsonPath("$.timeLine").value((Object) null)).andExpect(jsonPath("$.balanceLine.unitPrice").value(1_500_000))
                .andExpect(jsonPath("$.unbilled").value(1_500_000));

        String design = taskId(s.pm(), s.projectId(), "Design");
        assign(s.pm(), s.projectId(), design, s.devId(), 10).andExpect(status().isOk());
        startProject(s.pm(), s.projectId());
        moveTask(s.dev(), s.projectId(), design, "DOING").andExpect(status().isOk());
        postJson("/api/projects/" + s.projectId() + "/tasks/" + design + "/time", "{\"date\":\"" + day(0) + "\",\"hours\":2.5}", s.dev()).andExpect(status().isCreated());
        sentInvoice(s.pm(), s.projectId(), day(0), day(14), 400_000);

        getAs("/api/projects/" + s.projectId() + "/invoice-suggestion", adminToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedHours").value(2.5)).andExpect(jsonPath("$.timeLine.quantity").value(2.5))
                .andExpect(jsonPath("$.timeLine.unitPrice").value(25_000)).andExpect(jsonPath("$.timeLine.lineTotal").value(62_500))
                .andExpect(jsonPath("$.invoiced").value(400_000)).andExpect(jsonPath("$.balanceLine.lineTotal").value(1_100_000))
                .andExpect(jsonPath("$.projectName").value("Booking website"));
        getAs("/api/projects/00000000-0000-0000-0000-000000000001/invoice-suggestion", s.pm()).andExpect(status().isNotFound());
    }

    @Test
    void managementCanFilterAndExportInvoicesAsCsv() throws Exception {
        Scenario s = scenario();
        String companyId = companyIdOf(s.pm(), s.projectId());
        String a = sentInvoice(s.pm(), s.projectId(), "2045-02-01", "2045-02-20", 300_000);
        draftInvoice(s.pm(), s.projectId(), "2045-06-01", "2045-06-20", 50_000);
        postJson("/api/invoices", invoiceBody(null, companyId, "2045-08-01", "2045-08-20", "18", line("Domain renewal", "1", 25_000)), s.pm()).andExpect(status().isCreated());

        getAs("/api/invoices?companyId=" + companyId + "&from=2045-01-01&to=2045-12-31", s.pm()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].issueDate").value("2045-08-01"));
        getAs("/api/invoices?companyId=" + companyId + "&from=2045-05-01&to=2045-07-01", s.pm()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
        getAs("/api/invoices?companyId=" + companyId + "&status=DRAFT", s.pm()).andExpect(jsonPath("$", hasSize(2)));
        getAs("/api/invoices?from=2045-12-31&to=2045-01-01", s.pm()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RANGE"));
        getAs("/api/invoices?from=yesterday", s.pm()).andExpect(status().isBadRequest());

        getAs("/api/invoices/export.csv?companyId=" + companyId + "&from=2045-01-01&to=2045-12-31", s.pm()).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("invoices.csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Number,Status,Client,Project,Issue date,Due date,Subtotal,Tax,Total,Paid,Balance")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("INV-2045-")));
        getAs("/api/invoices/" + a, s.pm()).andExpect(status().isOk());
    }
}
