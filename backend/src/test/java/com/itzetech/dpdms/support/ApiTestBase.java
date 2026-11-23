package com.itzetech.dpdms.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Shared helpers: call the API like the frontend does and read values out of the JSON answers. */
@SpringBootTest(properties = {"app.expose-secrets=true", "app.jobs.enabled=false"})
@AutoConfigureMockMvc
@Import(TestDatabaseConfig.class)
public abstract class ApiTestBase {

    public static final String ADMIN_EMAIL = "admin@itzetech.local";
    public static final String ADMIN_FIRST_PASSWORD = "ChangeMe#2026";
    public static final String ADMIN_PASSWORD = "Admin-Pass-2026";
    public static final String GOOD_PASSWORD = "Sunrise2026";

    @Autowired
    protected MockMvc mvc;

    private static String cachedAdminToken;

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions postJson(String url, String json, String token) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json).header("Authorization", "Bearer " + token));
    }

    protected ResultActions getAs(String url, String token) throws Exception {
        MockHttpServletRequestBuilder b = get(url);
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(b);
    }

    protected static String read(ResultActions r, String path) throws Exception {
        Object v = JsonPath.read(r.andReturn().getResponse().getContentAsString(), path);
        return v == null ? null : v.toString();
    }

    protected static String json(String... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            String v = keyValues[i + 1];
            sb.append('"').append(keyValues[i]).append("\":");
            sb.append(v.equals("true") || v.equals("false") ? v : '"' + v.replace("\"", "\\\"") + '"');
        }
        return sb.append('}').toString();
    }

    /** Signs in as the bootstrap administrator, going through the first-time steps once. */
    protected synchronized String adminToken() throws Exception {
        if (cachedAdminToken != null) {
            return cachedAdminToken;
        }
        ResultActions login = postJson("/api/auth/login", json("email", ADMIN_EMAIL, "password", ADMIN_FIRST_PASSWORD));
        String status = read(login, "$.status");
        if ("PASSWORD_CHANGE_REQUIRED".equals(status)) {
            String challenge = read(login, "$.challenge");
            login = postJson("/api/auth/change-password", json("challenge", challenge, "newPassword", ADMIN_PASSWORD));
            status = read(login, "$.status");
        } else {
            login = postJson("/api/auth/login", json("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD));
            status = read(login, "$.status");
        }
        if ("CODE_REQUIRED".equals(status)) {
            login = postJson("/api/auth/verify-code", json("challenge", read(login, "$.challenge"), "code", read(login, "$.devCode")));
        }
        cachedAdminToken = read(login, "$.token");
        return cachedAdminToken;
    }

    /** Creates a staff member as the administrator and completes their first sign-in. Returns their access token. */
    protected String newStaffToken(String email, String role) throws Exception {
        ResultActions created = postJson("/api/admin/users", json("fullName", "Test " + role, "email", email, "role", role), adminToken());
        String temp = read(created, "$.temporaryPassword");
        ResultActions login = postJson("/api/auth/login", json("email", email, "password", temp));
        ResultActions changed = postJson("/api/auth/change-password", json("challenge", read(login, "$.challenge"), "newPassword", GOOD_PASSWORD));
        return read(changed, "$.token");
    }

    /** Registers a client and confirms the e-mail code. Returns their access token. */
    protected String newClientToken(String email, String company) throws Exception {
        ResultActions reg = postJson("/api/auth/register", json("companyName", company, "fullName", "Client " + company,
                "email", email, "password", GOOD_PASSWORD, "acceptTerms", "true"));
        ResultActions verified = postJson("/api/auth/verify-email", json("email", email, "code", read(reg, "$.devCode")));
        return read(verified, "$.token");
    }
}
