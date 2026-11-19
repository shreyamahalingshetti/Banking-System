package com.bank.banking_system.integration;

import com.bank.banking_system.dto.TransferRequest;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.Customer;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.CustomerRepository;
import com.bank.banking_system.service.TransactionService;
import com.bank.banking_system.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("20 simultaneous transfers maintain total money invariant and consistent balances")
    void concurrentTransfers_maintainTotalMoneyInvariant() throws Exception {
        Customer c1 = customerRepository.save(Customer.builder().fullName("Conc User 1").email("conc1_" + System.nanoTime() + "@bank.com").password("pwd").build());
        Customer c2 = customerRepository.save(Customer.builder().fullName("Conc User 2").email("conc2_" + System.nanoTime() + "@bank.com").password("pwd").build());

        Account accA = accountRepository.save(Account.builder()
                .accountNumber("CONC_A_" + System.nanoTime())
                .customer(c1)
                .balance(new BigDecimal("10000.00"))
                .status("ACTIVE")
                .build());

        Account accB = accountRepository.save(Account.builder()
                .accountNumber("CONC_B_" + System.nanoTime())
                .customer(c2)
                .balance(new BigDecimal("0.00"))
                .status("ACTIVE")
                .build());

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    transferService.transfer(TransferRequest.builder()
                            .fromAccountNumber(accA.getAccountNumber())
                            .toAccountNumber(accB.getAccountNumber())
                            .amount(new BigDecimal("1000.00"))
                            .build());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Account finalA = accountRepository.findByAccountNumber(accA.getAccountNumber()).orElseThrow();
        Account finalB = accountRepository.findByAccountNumber(accB.getAccountNumber()).orElseThrow();

        BigDecimal total = finalA.getBalance().add(finalB.getBalance());
        assertEquals(0, new BigDecimal("10000.00").compareTo(total), "Total money across A + B must remain exactly 10000.00");

        BigDecimal expectedA = new BigDecimal("10000.00").subtract(new BigDecimal("1000.00").multiply(new BigDecimal(successCount.get())));
        BigDecimal expectedB = new BigDecimal("1000.00").multiply(new BigDecimal(successCount.get()));

        assertEquals(0, expectedA.compareTo(finalA.getBalance()), "Balance A must match successful transfers");
        assertEquals(0, expectedB.compareTo(finalB.getBalance()), "Balance B must match successful transfers");
        assertTrue(finalA.getBalance().compareTo(BigDecimal.ZERO) >= 0, "Balance A cannot be negative");
    }

    @Test
    @DisplayName("50 concurrent withdrawals of 100.00 from 1000.00 balance: exactly 10 succeed and balance ends at 0.00")
    void concurrentWithdrawals_exactlyTenSucceed_andBalanceNeverNegative() throws Exception {
        Customer c = customerRepository.save(Customer.builder().fullName("Withdraw User").email("with_" + System.nanoTime() + "@bank.com").password("pwd").build());

        Account acc = accountRepository.save(Account.builder()
                .accountNumber("WITH_" + System.nanoTime())
                .customer(c)
                .balance(new BigDecimal("1000.00"))
                .status("ACTIVE")
                .build());

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    transactionService.withdraw(acc.getAccountNumber(), new BigDecimal("100.00"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Account finalAcc = accountRepository.findByAccountNumber(acc.getAccountNumber()).orElseThrow();

        assertEquals(10, successCount.get(), "Exactly 10 withdrawals of 100.00 must succeed from a 1000.00 balance");
        assertEquals(40, failCount.get(), "Exactly 40 withdrawals must fail due to insufficient funds");
        assertEquals(0, BigDecimal.ZERO.compareTo(finalAcc.getBalance()), "Final balance must be exactly 0.00");
    }
}
