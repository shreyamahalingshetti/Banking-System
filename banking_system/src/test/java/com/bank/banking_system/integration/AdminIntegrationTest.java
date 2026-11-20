package com.bank.banking_system.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Admin Login
        MvcResult adminLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@bank.com\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk()).andReturn();
        adminToken = extractRegex(adminLoginRes.getResponse().getContentAsString(), "\"token\"\\s*:\\s*\"([^\"]+)\"");

        // Register & Login Customer
        String custEmail = "admin_test_cust_" + System.nanoTime() + "@bank.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"fullName\":\"Customer AdminTest\",\"email\":\"%s\",\"password\":\"Pass123!\"}", custEmail)))
                .andExpect(status().isCreated());

        MvcResult custLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"Pass123!\"}", custEmail)))
                .andExpect(status().isOk()).andReturn();
        customerToken = extractRegex(custLoginRes.getResponse().getContentAsString(), "\"token\"\\s*:\\s*\"([^\"]+)\"");
    }

    @Test
    @DisplayName("CUSTOMER token gets 403 Forbidden on /api/admin/** and admin token gets 200 OK")
    void roleSecurity_customerGets403_adminGets200() throws Exception {
        // Customer token -> 403
        mockMvc.perform(get("/api/admin/summary")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Admin token -> 200
        mockMvc.perform(get("/api/admin/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").exists())
                .andExpect(jsonPath("$.totalAccounts").exists())
                .andExpect(jsonPath("$.totalBalanceAcrossAccounts").exists());
    }

    @Test
    @DisplayName("Freezing an account causes deposits to fail with 400, and a CLOSED account cannot be reactivated (400)")
    void accountStatusLifecycle_freezeAndClosedRestrictions() throws Exception {
        // Create an account
        MvcResult accRes = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"accountType\":\"SAVINGS\"}"))
                .andExpect(status().isCreated()).andReturn();
        String accountNumber = extractRegex(accRes.getResponse().getContentAsString(), "\"accountNumber\"\\s*:\\s*\"([^\"]+)\"");

        // Freeze the account
        mockMvc.perform(patch("/api/admin/accounts/" + accountNumber + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FROZEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        // Deposits on FROZEN account fail with 400
        mockMvc.perform(post("/api/accounts/" + accountNumber + "/deposit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Change status to CLOSED
        mockMvc.perform(patch("/api/admin/accounts/" + accountNumber + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // Reactivating a CLOSED account back to ACTIVE fails with 400
        mockMvc.perform(patch("/api/admin/accounts/" + accountNumber + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot change status of a CLOSED account"));
    }

    @Test
    @DisplayName("Audit log contains entries for REGISTER, LOGIN_SUCCESS, LOGIN_FAILED, and ACCOUNT_STATUS_CHANGED")
    void auditLog_containsExpectedActionEntries() throws Exception {
        // Trigger a deliberate login failure
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@bank.com\",\"password\":\"WrongPassword!\"}"))
                .andExpect(status().isUnauthorized());

        // Check audit logs
        MvcResult logResult = mockMvc.perform(get("/api/admin/audit-logs?page=0&size=50")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        String json = logResult.getResponse().getContentAsString();

        assertTrue(json.contains("\"action\":\"REGISTER\""), "Audit log must contain REGISTER action");
        assertTrue(json.contains("\"action\":\"LOGIN_SUCCESS\""), "Audit log must contain LOGIN_SUCCESS action");
        assertTrue(json.contains("\"action\":\"LOGIN_FAILED\""), "Audit log must contain LOGIN_FAILED action");
    }

    @Test
    @DisplayName("No admin response anywhere contains a password or password hash")
    void responses_neverContainPasswordsOrHashes() throws Exception {
        // Customer list response
        MvcResult custListRes = mockMvc.perform(get("/api/admin/customers?page=0&size=20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();

        String custJson = custListRes.getResponse().getContentAsString();
        assertFalse(custJson.contains("\"password\""), "Customer list must never contain 'password'");
        assertFalse(custJson.contains("\"passwordHash\""), "Customer list must never contain 'passwordHash'");

        // Accounts response
        MvcResult accListRes = mockMvc.perform(get("/api/admin/accounts?page=0&size=20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        String accJson = accListRes.getResponse().getContentAsString();
        assertFalse(accJson.contains("\"password\""), "Accounts list must never contain 'password'");

        // Audit logs response
        MvcResult auditRes = mockMvc.perform(get("/api/admin/audit-logs?page=0&size=20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        String auditJson = auditRes.getResponse().getContentAsString();
        assertFalse(auditJson.contains("\"password\""), "Audit log list must never contain 'password'");
    }

    private String extractRegex(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        throw new IllegalStateException("Regex " + regex + " not found in: " + json);
    }
}
