package com.bank.banking_system.integration;

import com.bank.banking_system.entity.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountTypeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String userToken;
    private Long customerId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "type_user_" + System.nanoTime() + "@bank.com";
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"fullName\":\"Account Type Tester\",\"email\":\"%s\",\"password\":\"TypePass123!\"}", email)))
                .andExpect(status().isCreated()).andReturn();

        Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
        Matcher idMatcher = idPattern.matcher(reg.getResponse().getContentAsString());
        if (idMatcher.find()) {
            customerId = Long.parseLong(idMatcher.group(1));
        }

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"TypePass123!\"}", email)))
                .andExpect(status().isOk()).andReturn();

        Pattern tokenPattern = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
        Matcher tokenMatcher = tokenPattern.matcher(login.getResponse().getContentAsString());
        if (tokenMatcher.find()) {
            userToken = tokenMatcher.group(1);
        }
    }

    @Test
    @DisplayName("Create an account with each valid AccountType (SAVINGS, CURRENT, SALARY, FIXED_DEPOSIT)")
    void createAccount_withEachType_returnsCreatedAndCorrectType() throws Exception {
        for (AccountType type : AccountType.values()) {
            mockMvc.perform(post("/api/accounts")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("{\"customerId\":%d,\"accountType\":\"%s\"}", customerId, type.name())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accountType").value(type.name()))
                    .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        // Verify all 4 accounts appear in /api/accounts/me
        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @DisplayName("Create an account with an invalid AccountType returns 400 Bad Request")
    void createAccount_invalidType_returns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"customerId\":%d,\"accountType\":\"NON_EXISTENT_TYPE\"}", customerId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Creating more than 5 accounts returns 400 with account limit error")
    void createAccount_moreThan5Accounts_returns400WithLimitMessage() throws Exception {
        // Create 5 accounts (allowed)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/accounts")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("{\"customerId\":%d,\"accountType\":\"SAVINGS\"}", customerId)))
                    .andExpect(status().isCreated());
        }

        // 6th account creation must fail with 400 Bad Request
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"customerId\":%d,\"accountType\":\"CURRENT\"}", customerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Customer cannot have more than 5 accounts")));
    }
}
