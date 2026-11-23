package com.itzetech.dpdms.auth;

import com.itzetech.dpdms.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTests extends ApiTestBase {

    @Test
    void clientRegistersConfirmsEmailAndSignsIn() throws Exception {
        String email = unique("jean") + "@acme.example";

        ResultActions reg = postJson("/api/auth/register", json("companyName", "Acme Hotels", "fullName", "Jean Bosco",
                "email", email, "phone", "0788", "password", GOOD_PASSWORD, "acceptTerms", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VERIFICATION_REQUIRED"));
        String code = read(reg, "$.devCode");

        // cannot sign in before the address is confirmed
        postJson("/api/auth/login", json("email", email, "password", GOOD_PASSWORD))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        // a wrong code is refused, the right one signs the client in
        postJson("/api/auth/verify-email", json("email", email, "code", "000000".equals(code) ? "111111" : "000000"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_INVALID"));
        ResultActions ok = postJson("/api/auth/verify-email", json("email", email, "code", code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.user.role").value("CLIENT")).andExpect(jsonPath("$.user.selfRegistered").value(true));
        String token = read(ok, "$.token");

        getAs("/api/auth/me", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.companyName").value("Acme Hotels"));

        // the code can be used only once, and now the client can sign in normally
        postJson("/api/auth/verify-email", json("email", email, "code", code)).andExpect(status().isBadRequest());
        postJson("/api/auth/login", json("email", email, "password", GOOD_PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTHENTICATED"));

        // administrators were told about the new client
        getAs("/api/notifications", adminToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].message", hasItem("New client registered: Acme Hotels (Jean Bosco).")));
    }

    @Test
    void registrationChecksTheForm() throws Exception {
        postJson("/api/auth/register", json("companyName", "", "fullName", "X", "email", "not-an-email", "password", "abc", "acceptTerms", "false"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.companyName").exists()).andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.acceptTerms").exists());
        postJson("/api/auth/register", json("companyName", "Acme", "fullName", "X Y", "email", unique("w") + "@acme.example",
                "password", "short1", "acceptTerms", "true"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));

        String email = unique("dup") + "@acme.example";
        newClientToken(email, "Dup Co");
        postJson("/api/auth/register", json("companyName", "Dup Co", "fullName", "X Y", "email", email.toUpperCase(),
                "password", GOOD_PASSWORD, "acceptTerms", "true"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void administratorSignsInWithPasswordThenEmailedCode() throws Exception {
        getAs("/api/auth/me", adminToken()).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));

        // a second administrator: the password alone never gives access to the API
        String email = unique("admin2") + "@itzetech.example";
        ResultActions created = postJson("/api/admin/users", json("fullName", "Second Admin", "email", email, "role", "ADMIN"), adminToken())
                .andExpect(status().isCreated());
        ResultActions first = postJson("/api/auth/login", json("email", email, "password", read(created, "$.temporaryPassword")));
        ResultActions changed = postJson("/api/auth/change-password", json("challenge", read(first, "$.challenge"), "newPassword", GOOD_PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CODE_REQUIRED"))
                .andExpect(jsonPath("$.token").doesNotExist());
        String challenge = read(changed, "$.challenge");
        String code = read(changed, "$.devCode");

        postJson("/api/auth/verify-code", json("challenge", challenge, "code", "000000".equals(code) ? "111111" : "000000"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_INVALID"));
        ResultActions done = postJson("/api/auth/verify-code", json("challenge", challenge, "code", code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
        getAs("/api/admin/users", read(done, "$.token")).andExpect(status().isOk());
    }

    @Test
    void stepTokensCannotBeUsedToCallTheApi() throws Exception {
        String email = unique("inv") + "@itzetech.example";
        ResultActions created = postJson("/api/admin/users", json("fullName", "Invited Dev", "email", email, "role", "DEVELOPER"), adminToken());
        ResultActions login = postJson("/api/auth/login", json("email", email, "password", read(created, "$.temporaryPassword")));
        getAs("/api/auth/me", read(login, "$.challenge")).andExpect(status().isUnauthorized());
    }

    @Test
    void invitedStaffMustChooseANewPasswordAtFirstSignIn() throws Exception {
        String email = unique("dev") + "@itzetech.example";
        ResultActions created = postJson("/api/admin/users", json("fullName", "New Developer", "email", email, "role", "DEVELOPER"), adminToken())
                .andExpect(status().isCreated()).andExpect(jsonPath("$.user.status").value("INVITED"))
                .andExpect(jsonPath("$.user.role").value("DEVELOPER"));
        String temp = read(created, "$.temporaryPassword");

        ResultActions login = postJson("/api/auth/login", json("email", email, "password", temp))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PASSWORD_CHANGE_REQUIRED"));
        String challenge = read(login, "$.challenge");

        postJson("/api/auth/change-password", json("challenge", challenge, "newPassword", temp))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SAME_PASSWORD"));
        postJson("/api/auth/change-password", json("challenge", challenge, "newPassword", "weak"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
        postJson("/api/auth/change-password", json("challenge", challenge, "newPassword", GOOD_PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));

        // the temporary password no longer works; the new one does
        postJson("/api/auth/login", json("email", email, "password", temp)).andExpect(status().isUnauthorized());
        postJson("/api/auth/login", json("email", email, "password", GOOD_PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordsLockTheAccountForAWhile() throws Exception {
        String email = unique("lock") + "@acme.example";
        newClientToken(email, "Lock Co");
        for (int i = 0; i < 5; i++) {
            postJson("/api/auth/login", json("email", email, "password", "Wrong-Password-1")).andExpect(status().isUnauthorized());
        }
        postJson("/api/auth/login", json("email", email, "password", GOOD_PASSWORD))
                .andExpect(status().isLocked()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        // an unknown email gets the same answer as a wrong password
        postJson("/api/auth/login", json("email", "nobody@nowhere.example", "password", "Whatever-1"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void protectedEndpointsNeedASignIn() throws Exception {
        getAs("/api/auth/me", null).andExpect(status().isUnauthorized());
        getAs("/api/notifications", null).andExpect(status().isUnauthorized());
        getAs("/api/admin/users", null).andExpect(status().isUnauthorized());
        // the notifications list of a brand-new client is empty
        String token = newClientToken(unique("n") + "@acme.example", "Note Co");
        getAs("/api/notifications", token).andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.unread").value(0));
    }
}
