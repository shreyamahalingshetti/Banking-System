package com.bank.banking_system.integration;

import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.EntryType;
import com.bank.banking_system.entity.LedgerEntry;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.LedgerEntryRepository;
import com.bank.banking_system.repository.TransferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    @DisplayName("Full transfer flow verifies final balances, one transfer record, two ledger entries, and unchanged balance on failure")
    void fullTransferFlow_verifiesLedgerAndBalances() throws Exception {
        // 1. Register and login Customer 1
        String email1 = "tx_cust1_" + System.nanoTime() + "@bank.com";
        MvcResult reg1 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"fullName\":\"Customer One\",\"email\":\"%s\",\"password\":\"Pass12345\"}", email1)))
                .andExpect(status().isCreated()).andReturn();
        long cust1Id = Long.parseLong(extractRegex(reg1.getResponse().getContentAsString(), "\"id\"\\s*:\\s*(\\d+)"));

        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"Pass12345\"}", email1)))
                .andExpect(status().isOk()).andReturn();
        String token1 = extractRegex(login1.getResponse().getContentAsString(), "\"token\"\\s*:\\s*\"([^\"]+)\"");

        // 2. Register and login Customer 2
        String email2 = "tx_cust2_" + System.nanoTime() + "@bank.com";
        MvcResult reg2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"fullName\":\"Customer Two\",\"email\":\"%s\",\"password\":\"Pass12345\"}", email2)))
                .andExpect(status().isCreated()).andReturn();
        long cust2Id = Long.parseLong(extractRegex(reg2.getResponse().getContentAsString(), "\"id\"\\s*:\\s*(\\d+)"));

        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"Pass12345\"}", email2)))
                .andExpect(status().isOk()).andReturn();
        String token2 = extractRegex(login2.getResponse().getContentAsString(), "\"token\"\\s*:\\s*\"([^\"]+)\"");

        // 3. Create accounts
        MvcResult acc1Res = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"customerId\":%d}", cust1Id)))
                .andExpect(status().isCreated()).andReturn();
        String accNum1 = extractRegex(acc1Res.getResponse().getContentAsString(), "\"accountNumber\"\\s*:\\s*\"([^\"]+)\"");

        MvcResult acc2Res = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"customerId\":%d}", cust2Id)))
                .andExpect(status().isCreated()).andReturn();
        String accNum2 = extractRegex(acc2Res.getResponse().getContentAsString(), "\"accountNumber\"\\s*:\\s*\"([^\"]+)\"");

        // 4. Deposit 1000.00 into account 1
        mockMvc.perform(post("/api/accounts/" + accNum1 + "/deposit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(1000.00));

        long initialTransferCount = transferRepository.count();
        long initialLedgerCount = ledgerEntryRepository.count();

        // 5. Transfer 400.00 from account 1 to account 2
        String transferBody = String.format("{\"fromAccountNumber\":\"%s\",\"toAccountNumber\":\"%s\",\"amount\":400.00}",
                accNum1, accNum2);

        MvcResult transferRes = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(400.00))
                .andReturn();

        String transactionId = extractRegex(transferRes.getResponse().getContentAsString(), "\"transactionId\"\\s*:\\s*\"([^\"]+)\"");
        assertNotNull(transactionId);

        // 6. Verify final balances in database
        Account reloaded1 = accountRepository.findByAccountNumber(accNum1).orElseThrow();
        Account reloaded2 = accountRepository.findByAccountNumber(accNum2).orElseThrow();
        assertEquals(0, new BigDecimal("600.00").compareTo(reloaded1.getBalance()), "Account 1 balance should be 600.00");
        assertEquals(0, new BigDecimal("400.00").compareTo(reloaded2.getBalance()), "Account 2 balance should be 400.00");

        // 7. Verify transfer table has exactly 1 new row
        long finalTransferCount = transferRepository.count();
        assertEquals(initialTransferCount + 1, finalTransferCount, "Exactly one transfer record should be created");

        // 8. Verify exactly two ledger_entries rows sharing the same transactionId
        long finalLedgerCount = ledgerEntryRepository.count();
        assertEquals(initialLedgerCount + 2, finalLedgerCount, "Exactly two ledger entries should be created");

        List<LedgerEntry> entries = ledgerEntryRepository.findAll().stream()
                .filter(e -> transactionId.equals(e.getTransactionId()))
                .toList();
        assertEquals(2, entries.size(), "Two ledger entries must share the transactionId");

        LedgerEntry debit = entries.stream().filter(e -> e.getEntryType() == EntryType.DEBIT).findFirst().orElseThrow();
        LedgerEntry credit = entries.stream().filter(e -> e.getEntryType() == EntryType.CREDIT).findFirst().orElseThrow();

        assertEquals(reloaded1.getId(), debit.getAccount().getId());
        assertEquals(0, new BigDecimal("400.00").compareTo(debit.getAmount()));
        assertEquals(0, new BigDecimal("600.00").compareTo(debit.getBalanceAfter()));

        assertEquals(reloaded2.getId(), credit.getAccount().getId());
        assertEquals(0, new BigDecimal("400.00").compareTo(credit.getAmount()));
        assertEquals(0, new BigDecimal("400.00").compareTo(credit.getBalanceAfter()));

        // 9. Verify a failed transfer leaves balances completely unchanged
        String excessiveTransferBody = String.format("{\"fromAccountNumber\":\"%s\",\"toAccountNumber\":\"%s\",\"amount\":9999.00}",
                accNum1, accNum2);

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(excessiveTransferBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Account reloadedAfterFail1 = accountRepository.findByAccountNumber(accNum1).orElseThrow();
        Account reloadedAfterFail2 = accountRepository.findByAccountNumber(accNum2).orElseThrow();
        assertEquals(0, new BigDecimal("600.00").compareTo(reloadedAfterFail1.getBalance()), "Balance 1 unchanged after failed transfer");
        assertEquals(0, new BigDecimal("400.00").compareTo(reloadedAfterFail2.getBalance()), "Balance 2 unchanged after failed transfer");
    }

    private String extractRegex(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        throw new IllegalStateException("Regex " + regex + " not found in: " + json);
    }
}
