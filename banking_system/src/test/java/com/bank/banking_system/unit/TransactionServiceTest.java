package com.bank.banking_system.unit;

import com.bank.banking_system.dto.TransactionResponse;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.Customer;
import com.bank.banking_system.entity.Transaction;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import com.bank.banking_system.exception.InsufficientFundsException;
import com.bank.banking_system.exception.InvalidOperationException;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.TransactionRepository;
import com.bank.banking_system.service.TransactionRecordService;
import com.bank.banking_system.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionRecordService transactionRecordService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("Deposit increases balance and saves a SUCCESS transaction record")
    void deposit_increasesBalance_andSavesSuccess() {
        Customer customer = Customer.builder().id(1L).email("owner@test.com").build();
        Account account = Account.builder()
                .id(1L)
                .accountNumber("111122223333")
                .customer(customer)
                .balance(new BigDecimal("100.00"))
                .status("ACTIVE")
                .build();

        when(accountRepository.findByAccountNumberWithLock("111122223333")).thenReturn(Optional.of(account));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(100L);
            tx.setCreatedAt(LocalDateTime.now());
            return tx;
        });

        TransactionResponse response = transactionService.deposit("111122223333", new BigDecimal("50.00"));

        assertNotNull(response);
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(TransactionType.DEPOSIT, response.getType());
        assertEquals(new BigDecimal("150.00"), response.getBalanceAfter());
        assertEquals(new BigDecimal("150.00"), account.getBalance());

        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionRecordService, never()).recordFailedTransaction(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Withdraw with insufficient funds throws InsufficientFundsException and records FAILED transaction")
    void withdraw_insufficientFunds_throwsException_andRecordsFailed() {
        Customer customer = Customer.builder().id(1L).email("owner@test.com").build();
        Account account = Account.builder()
                .id(1L)
                .accountNumber("111122223333")
                .customer(customer)
                .balance(new BigDecimal("20.00"))
                .status("ACTIVE")
                .build();

        when(accountRepository.findByAccountNumberWithLock("111122223333")).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class, () ->
                transactionService.withdraw("111122223333", new BigDecimal("50.00")));

        assertEquals(new BigDecimal("20.00"), account.getBalance()); // Balance unchanged
        verify(transactionRecordService).recordFailedTransaction(
                eq(account), eq(TransactionType.WITHDRAWAL), eq(new BigDecimal("50.00")), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Operations on a non-ACTIVE (e.g. FROZEN or CLOSED) account are rejected")
    void operations_onNonActiveAccount_rejected() {
        Customer customer = Customer.builder().id(1L).email("owner@test.com").build();
        Account frozenAccount = Account.builder()
                .id(1L)
                .accountNumber("111122223333")
                .customer(customer)
                .balance(new BigDecimal("200.00"))
                .status("FROZEN")
                .build();

        when(accountRepository.findByAccountNumberWithLock("111122223333")).thenReturn(Optional.of(frozenAccount));

        // Test deposit rejection
        assertThrows(InvalidOperationException.class, () ->
                transactionService.deposit("111122223333", new BigDecimal("50.00")));
        verify(transactionRecordService).recordFailedTransaction(
                eq(frozenAccount), eq(TransactionType.DEPOSIT), eq(new BigDecimal("50.00")), any());

        // Test withdraw rejection
        assertThrows(InvalidOperationException.class, () ->
                transactionService.withdraw("111122223333", new BigDecimal("50.00")));
        verify(transactionRecordService).recordFailedTransaction(
                eq(frozenAccount), eq(TransactionType.WITHDRAWAL), eq(new BigDecimal("50.00")), any());
    }
}
