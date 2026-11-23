package com.itzetech.dpdms.user;

import com.itzetech.dpdms.support.DeliveryTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaffListTests extends DeliveryTestBase {

    @Test
    void managersAndAdministratorsCanPickPeopleForTasks() throws Exception {
        String pm = newPm();
        String dev = newDev();
        String devId = meId(dev);
        String clientToken = newClient("Staff Co");

        getAs("/api/staff?role=DEVELOPER", pm).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(devId))).andExpect(jsonPath("$[*].role", everyItem(equalTo("DEVELOPER"))))
                .andExpect(jsonPath("$[0].id").exists()).andExpect(jsonPath("$[0].fullName").exists())
                .andExpect(jsonPath("$[0].email").exists()).andExpect(jsonPath("$[0].role").exists());
        getAs("/api/staff?role=DEVELOPER", adminToken()).andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(devId)));

        // without a role: all active staff, never clients
        getAs("/api/staff", pm).andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(devId)))
                .andExpect(jsonPath("$[*].id", hasItem(meId(pm)))).andExpect(jsonPath("$[*].id", not(hasItem(meId(clientToken)))))
                .andExpect(jsonPath("$[*].role", not(hasItem("CLIENT"))));
        getAs("/api/staff?role=PROJECT_MANAGER", pm).andExpect(jsonPath("$[*].id", hasItem(meId(pm)))).andExpect(jsonPath("$[*].id", not(hasItem(devId))));
    }

    @Test
    void onlyIdNameEmailAndRoleAreSent() throws Exception {
        String pm = newPm();
        newDev();
        getAs("/api/staff?role=DEVELOPER", pm).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").doesNotExist()).andExpect(jsonPath("$[0].lastLoginAt").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist()).andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("temporaryPassword"))));
    }

    @Test
    void developersClientsAndStrangersAreRefused() throws Exception {
        getAs("/api/staff", newDev()).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        getAs("/api/staff?role=DEVELOPER", newDev()).andExpect(status().isForbidden());
        getAs("/api/staff?role=DEVELOPER", newClient("Nosy Staff Co")).andExpect(status().isForbidden());
        getAs("/api/staff", null).andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedAndInvitedPeopleAreLeftOut() throws Exception {
        String pm = newPm();
        String dev = newDev();
        String devId = meId(dev);
        // invited but never signed in
        String invitedId = read(postJson("/api/admin/users", json("fullName", "Not Yet", "email", unique("inv") + "@itzetech.example", "role", "DEVELOPER"),
                adminToken()).andExpect(status().isCreated()), "$.user.id");

        getAs("/api/staff?role=DEVELOPER", pm).andExpect(jsonPath("$[*].id", hasItem(devId))).andExpect(jsonPath("$[*].id", not(hasItem(invitedId))));

        postJson("/api/admin/users/" + devId + "/deactivate", "{}", adminToken()).andExpect(status().isOk());
        getAs("/api/staff?role=DEVELOPER", pm).andExpect(jsonPath("$[*].id", not(hasItem(devId))));
        getAs("/api/staff", pm).andExpect(jsonPath("$[*].id", not(hasItem(devId))));
        postJson("/api/admin/users/" + devId + "/reactivate", "{}", adminToken()).andExpect(status().isOk());
        getAs("/api/staff?role=DEVELOPER", pm).andExpect(jsonPath("$[*].id", hasItem(devId)));
    }

    @Test
    void theRoleFilterIsChecked() throws Exception {
        String pm = newPm();
        getAs("/api/staff?role=CLIENT", pm).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ROLE_NOT_STAFF"));
        getAs("/api/staff?role=BOSS", pm).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
