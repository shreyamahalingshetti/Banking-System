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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String tokenA;
    private String tokenB;
    private Long customerIdA;
    private Long customerIdB;
    private String accountA;
    private String accountB;

    @BeforeEach
    void setUp() throws Exception {
        // Register & login customer A
        String emailA = "custA_" + System.nanoTime() + "@bank.com";
        MvcResult regA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"fullName\":\"Customer A\",\"email\":\"%s\",\"password\":\"PassA123!\"}", emailA)))
                .andExpect(status().isCreated()).andReturn();
        customerIdA = Long.parseLong(extractRegex(regA.getResponse().getContentAsString(), "\"id\"\\s*:\\s*(\\d+)"));

        MvcResult loginA = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"PassA123!\"}", emailA)))
                .andExpect(status().isOk()).andReturn();
        tokenA = extractRegex(loginA.getResponse().getContentAsString(), "\"token\"\\s*:\\s*\"([^\"]+)\"");

        // Create Account for A
        MvcResult accARes = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"customerId\":%d,\"accountType\":\"SAVINGS\"}", customerIdA)))
                .andExpect(status().isCreated()).andReturn();
        accountA = extractRegex(accARes.getResponse().getContentAsString(), "\"accountNumber\"\\s*:\\s*\"([^\"]+)\"");

        // Register & login customer B
        String emailB = "custB_" + System.nanoTime() + "@bank.com";
        MvcResult regB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"fullName\":\"Customer B\",\"email\":\"%s\",\"password\":\"PassB123!\"}", emailB)))
                .andExpect(status().isCreated()).andReturn();
        customerIdB = Long.parseLong(extractRegex(regB.getResponse().getContentAsString(), "\"id\"\\s*:\\s*(\\d+)"));

        MvcResult loginB = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"PassB123!\"}", emailB)))
                .andExpect(status().isOk()).andReturn();
        tokenB = extractRegex(loginB.getResponse().getContentAsString(), "\"token\"\\s*:\\s*\"([^\"]+)\"");

        // Create Account for B
        MvcResult accBRes = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"customerId\":%d,\"accountType\":\"SAVINGS\"}", customerIdB)))
                .andExpect(status().isCreated()).andReturn();
        accountB = extractRegex(accBRes.getResponse().getContentAsString(), "\"accountNumber\"\\s*:\\s*\"([^\"]+)\"");

        // Deposit initial funds into account A by customer A
        mockMvc.perform(post("/api/accounts/" + accountA + "/deposit")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000.00}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("No token returns 401 Unauthorized for account operations")
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/accounts/" + accountA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/accounts/" + accountA + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Customer A cannot view Customer B's account (403)")
    void customerA_cannotView_customerBAccount_returns403() throws Exception {
        mockMvc.perform(get("/api/accounts/" + accountB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Customer A cannot deposit into Customer B's account (403)")
    void customerA_cannotDeposit_customerBAccount_returns403() throws Exception {
        mockMvc.perform(post("/api/accounts/" + accountB + "/deposit")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Customer A cannot withdraw from Customer B's account (403)")
    void customerA_cannotWithdraw_customerBAccount_returns403() throws Exception {
        mockMvc.perform(post("/api/accounts/" + accountB + "/withdraw")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Customer A cannot transfer out of Customer B's account (403)")
    void customerA_cannotTransferOut_customerBAccount_returns403() throws Exception {
        String transferBody = String.format("{\"fromAccountNumber\":\"%s\",\"toAccountNumber\":\"%s\",\"amount\":50.00}",
                accountB, accountA);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Customer A can transfer into Customer B's account")
    void customerA_canTransferInto_customerBAccount() throws Exception {
        String transferBody = String.format("{\"fromAccountNumber\":\"%s\",\"toAccountNumber\":\"%s\",\"amount\":150.00}",
                accountA, accountB);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fromAccountNumber").value(accountA))
                .andExpect(jsonPath("$.toAccountNumber").value(accountB))
                .andExpect(jsonPath("$.amount").value(150.00));

        // Verify Customer A can view account A balance (1000 - 150 = 850.00)
        mockMvc.perform(get("/api/accounts/" + accountA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(850.00));

        // Verify Customer B can view account B balance (0 + 150 = 150.00)
        mockMvc.perform(get("/api/accounts/" + accountB)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    private String extractRegex(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        throw new IllegalStateException("Regex " + regex + " not found in: " + json);
    }
}
