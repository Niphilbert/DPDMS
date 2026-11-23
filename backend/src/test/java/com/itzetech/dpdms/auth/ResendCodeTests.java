package com.itzetech.dpdms.auth;

import com.itzetech.dpdms.support.DeliveryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResendCodeTests extends DeliveryTestBase {

    private String register(String email) throws Exception {
        ResultActions reg = postJson("/api/auth/register", json("companyName", "Resend Co", "fullName", "Re Send", "email", email,
                "password", GOOD_PASSWORD, "acceptTerms", "true")).andExpect(status().isCreated());
        return read(reg, "$.devCode");
    }

    @Test
    void askingAgainTooSoonIsRefusedCleanlyAndTheFirstCodeStillWorks() throws Exception {
        String email = unique("resend") + "@client.example";
        String code = register(email);

        // inside the 60 seconds: a clear 429, not a server error
        postJson("/api/auth/resend-code", json("email", email)).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("CODE_TOO_SOON"));
        postJson("/api/auth/resend-code", json("email", email)).andExpect(status().isTooManyRequests());

        // the refusal changed nothing: the code sent at registration is still the valid one
        postJson("/api/auth/verify-email", json("email", email, "code", code)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"));
    }

    @Test
    void afterTheWaitANewCodeIsIssuedAndShownInDevelopment() throws Exception {
        String email = unique("again") + "@client.example";
        String first = register(email);
        jdbc.update("update one_time_code set created_at = created_at - interval '2 minutes' where user_id = "
                + "(select id from user_account where email = ?)", email);

        String second = read(postJson("/api/auth/resend-code", json("email", email)).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.devCode").exists()), "$.devCode");
        // the old code no longer works, the new one does
        if (!first.equals(second)) {
            postJson("/api/auth/verify-email", json("email", email, "code", first)).andExpect(status().isBadRequest());
        }
        postJson("/api/auth/verify-email", json("email", email, "code", second)).andExpect(status().isOk());
    }

    @Test
    void unknownAndAlreadyConfirmedAddressesGetTheSameNeutralAnswer() throws Exception {
        postJson("/api/auth/resend-code", json("email", "nobody-" + unique("x") + "@client.example")).andExpect(status().isAccepted())
                .andExpect(content().string(""));
        String confirmed = unique("done") + "@client.example";
        newClientToken(confirmed, "Done Co");
        postJson("/api/auth/resend-code", json("email", confirmed)).andExpect(status().isAccepted()).andExpect(content().string(""));
        postJson("/api/auth/resend-code", json("email", "not-an-email")).andExpect(status().isBadRequest());
    }
}
