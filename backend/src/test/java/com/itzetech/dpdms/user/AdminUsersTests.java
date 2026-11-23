package com.itzetech.dpdms.user;

import com.itzetech.dpdms.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUsersTests extends ApiTestBase {

    private ResultActions patchRole(String id, String role) throws Exception {
        return mvc.perform(patch("/api/admin/users/" + id + "/role").contentType("application/json")
                .content(json("role", role)).header("Authorization", "Bearer " + adminTokenUnchecked()));
    }

    private String adminTokenUnchecked() throws Exception {
        return adminToken();
    }

    @Test
    void onlyTheAdministratorCanManageAccounts() throws Exception {
        String dev = newStaffToken(unique("d") + "@itzetech.example", "DEVELOPER");
        String pm = newStaffToken(unique("p") + "@itzetech.example", "PROJECT_MANAGER");
        String client = newClientToken(unique("c") + "@acme.example", "Acme");
        for (String token : new String[]{dev, pm, client}) {
            getAs("/api/admin/users", token).andExpect(status().isForbidden());
            getAs("/api/admin/audit", token).andExpect(status().isForbidden());
            postJson("/api/admin/users", json("fullName", "X Y", "email", unique("z") + "@itzetech.example", "role", "DEVELOPER"), token)
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void staffCannotBeCreatedAsClientsAndEmailsMustBeUnique() throws Exception {
        postJson("/api/admin/users", json("fullName", "Some One", "email", unique("c") + "@itzetech.example", "role", "CLIENT"), adminToken())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ROLE_NOT_STAFF"));
        String email = unique("same") + "@itzetech.example";
        postJson("/api/admin/users", json("fullName", "One Two", "email", email, "role", "DEVELOPER"), adminToken()).andExpect(status().isCreated());
        postJson("/api/admin/users", json("fullName", "One Two", "email", email, "role", "PROJECT_MANAGER"), adminToken())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        postJson("/api/admin/users", json("fullName", "", "email", "bad", "role", "DEVELOPER"), adminToken())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void deactivatingAnAccountStopsItImmediately() throws Exception {
        String email = unique("gone") + "@itzetech.example";
        String token = newStaffToken(email, "DEVELOPER");
        String id = read(getAs("/api/auth/me", token), "$.id");
        getAs("/api/auth/me", token).andExpect(status().isOk());

        postJson("/api/admin/users/" + id + "/deactivate", "{}", adminToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEACTIVATED"));
        // the token they already hold stops working at once, and they cannot sign in again
        getAs("/api/auth/me", token).andExpect(status().isUnauthorized());
        postJson("/api/auth/login", json("email", email, "password", GOOD_PASSWORD))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"));

        postJson("/api/admin/users/" + id + "/reactivate", "{}", adminToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        postJson("/api/auth/login", json("email", email, "password", GOOD_PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void rolesCanBeChangedForStaffOnly() throws Exception {
        String dev = newStaffToken(unique("r") + "@itzetech.example", "DEVELOPER");
        String devId = read(getAs("/api/auth/me", dev), "$.id");
        patchRole(devId, "PROJECT_MANAGER").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("PROJECT_MANAGER"));
        patchRole(devId, "CLIENT").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ROLE_NOT_STAFF"));

        String client = newClientToken(unique("rc") + "@acme.example", "Role Co");
        String clientId = read(getAs("/api/auth/me", client), "$.id");
        patchRole(clientId, "DEVELOPER").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ROLE_NOT_STAFF"));
    }

    @Test
    void administratorsCannotLockThemselvesOut() throws Exception {
        String myId = read(getAs("/api/auth/me", adminToken()), "$.id");
        postJson("/api/admin/users/" + myId + "/deactivate", "{}", adminToken())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SELF_NOT_ALLOWED"));
        patchRole(myId, "DEVELOPER").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SELF_NOT_ALLOWED"));
    }

    @Test
    void passwordResetForcesANewPasswordAndEverythingIsAudited() throws Exception {
        String email = unique("reset") + "@itzetech.example";
        String token = newStaffToken(email, "PROJECT_MANAGER");
        String id = read(getAs("/api/auth/me", token), "$.id");

        ResultActions reset = postJson("/api/admin/users/" + id + "/reset-password", "{}", adminToken())
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.status").value("INVITED"));
        String temp = read(reset, "$.temporaryPassword");
        postJson("/api/auth/login", json("email", email, "password", GOOD_PASSWORD)).andExpect(status().isUnauthorized());
        postJson("/api/auth/login", json("email", email, "password", temp))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PASSWORD_CHANGE_REQUIRED"));

        getAs("/api/admin/audit?limit=200", adminToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", hasItem("Password reset")))
                .andExpect(jsonPath("$[*].action", hasItem("User created")))
                .andExpect(jsonPath("$[*].action", hasItem("Signed in")));
    }

    @Test
    void usersCanBeSearchedAndFilteredByRole() throws Exception {
        String name = unique("findme");
        postJson("/api/admin/users", json("fullName", "Findable Person", "email", name + "@itzetech.example", "role", "DEVELOPER"), adminToken());
        getAs("/api/admin/users?q=" + name, adminToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(name + "@itzetech.example"));
        getAs("/api/admin/users?role=ADMIN", adminToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].role", hasItem("ADMIN")));
        getAs("/api/admin/users?role=CLIENT", adminToken()).andExpect(status().isOk());
    }
}
