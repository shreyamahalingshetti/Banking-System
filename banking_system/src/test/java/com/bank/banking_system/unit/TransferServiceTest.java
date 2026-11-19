package com.bank.banking_system.unit;

import com.bank.banking_system.dto.TransferRequest;
import com.bank.banking_system.dto.TransferResponse;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.Customer;
import com.bank.banking_system.entity.EntryType;
import com.bank.banking_system.entity.LedgerEntry;
import com.bank.banking_system.entity.Transaction;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransferRecord;
import com.bank.banking_system.exception.InsufficientFundsException;
import com.bank.banking_system.exception.InvalidOperationException;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.LedgerEntryRepository;
import com.bank.banking_system.repository.TransactionRepository;
import com.bank.banking_system.repository.TransferRepository;
import com.bank.banking_system.service.TransferRecordService;
import com.bank.banking_system.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferRecordService transferRecordService;

    @InjectMocks
    private TransferService transferService;

    @Test
    @DisplayName("Successful transfer debits and credits right amounts and saves matching DEBIT and CREDIT ledger entries")
    void transfer_success_debitsAndCredits_andSavesLedgerEntries() {
        Customer c1 = Customer.builder().id(1L).email("alice@test.com").build();
        Customer c2 = Customer.builder().id(2L).email("bob@test.com").build();

        Account fromAcc = Account.builder()
                .id(1L)
                .accountNumber("100000000001")
                .customer(c1)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build();

        Account toAcc = Account.builder()
                .id(2L)
                .accountNumber("200000000002")
                .customer(c2)
                .balance(new BigDecimal("200.00"))
                .status("ACTIVE")
                .build();

        when(accountRepository.findByAccountNumberWithLock("100000000001")).thenReturn(Optional.of(fromAcc));
        when(accountRepository.findByAccountNumberWithLock("200000000002")).thenReturn(Optional.of(toAcc));

        when(transferRepository.save(any(TransferRecord.class))).thenAnswer(invocation -> {
            TransferRecord tr = invocation.getArgument(0);
            tr.setId(10L);
            tr.setCreatedAt(LocalDateTime.now());
            return tr;
        });

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("100000000001")
                .toAccountNumber("200000000002")
                .amount(new BigDecimal("150.00"))
                .build();

        TransferResponse response = transferService.transfer(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals("100000000001", response.getFromAccountNumber());
        assertEquals("200000000002", response.getToAccountNumber());
        assertEquals(new BigDecimal("150.00"), response.getAmount());

        // Balances updated correctly
        assertEquals(new BigDecimal("350.00"), fromAcc.getBalance());
        assertEquals(new BigDecimal("350.00"), toAcc.getBalance());

        // Verify two ledger entries saved with the same transactionId
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(ledgerCaptor.capture());
        List<LedgerEntry> entries = ledgerCaptor.getAllValues();

        assertEquals(2, entries.size());
        assertEquals(entries.get(0).getTransactionId(), entries.get(1).getTransactionId(),
                "Both ledger entries must share the same transactionId");

        LedgerEntry debit = entries.stream().filter(e -> e.getEntryType() == EntryType.DEBIT).findFirst().orElseThrow();
        LedgerEntry credit = entries.stream().filter(e -> e.getEntryType() == EntryType.CREDIT).findFirst().orElseThrow();

        assertEquals("100000000001", debit.getAccount().getAccountNumber());
        assertEquals(new BigDecimal("150.00"), debit.getAmount());
        assertEquals(new BigDecimal("350.00"), debit.getBalanceAfter());

        assertEquals("200000000002", credit.getAccount().getAccountNumber());
        assertEquals(new BigDecimal("150.00"), credit.getAmount());
        assertEquals(new BigDecimal("350.00"), credit.getBalanceAfter());

        // Verify two transaction history rows saved
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Same-account transfer is rejected with InvalidOperationException")
    void transfer_sameAccount_rejected() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("100000000001")
                .toAccountNumber("100000000001")
                .amount(new BigDecimal("50.00"))
                .build();

        assertThrows(InvalidOperationException.class, () -> transferService.transfer(request));
        verify(accountRepository, never()).findByAccountNumberWithLock(any());
    }

    @Test
    @DisplayName("Insufficient funds rejected with InsufficientFundsException and FAILED record is saved")
    void transfer_insufficientFunds_rejected() {
        Customer c1 = Customer.builder().id(1L).email("alice@test.com").build();
        Customer c2 = Customer.builder().id(2L).email("bob@test.com").build();

        Account fromAcc = Account.builder()
                .id(1L)
                .accountNumber("100000000001")
                .customer(c1)
                .balance(new BigDecimal("20.00"))
                .status("ACTIVE")
                .build();

        Account toAcc = Account.builder()
                .id(2L)
                .accountNumber("200000000002")
                .customer(c2)
                .balance(new BigDecimal("200.00"))
                .status("ACTIVE")
                .build();

        when(accountRepository.findByAccountNumberWithLock("100000000001")).thenReturn(Optional.of(fromAcc));
        when(accountRepository.findByAccountNumberWithLock("200000000002")).thenReturn(Optional.of(toAcc));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("100000000001")
                .toAccountNumber("200000000002")
                .amount(new BigDecimal("50.00"))
                .build();

        assertThrows(InsufficientFundsException.class, () -> transferService.transfer(request));

        verify(transferRecordService).recordFailedTransfer(
                any(), eq(fromAcc), eq(toAcc), eq(new BigDecimal("50.00")), any());
        assertEquals(new BigDecimal("20.00"), fromAcc.getBalance());
        assertEquals(new BigDecimal("200.00"), toAcc.getBalance());
    }

    @Test
    @DisplayName("Non-ACTIVE account is rejected with InvalidOperationException and FAILED record")
    void transfer_nonActiveAccount_rejected() {
        Customer c1 = Customer.builder().id(1L).email("alice@test.com").build();
        Customer c2 = Customer.builder().id(2L).email("bob@test.com").build();

        Account fromAcc = Account.builder()
                .id(1L)
                .accountNumber("100000000001")
                .customer(c1)
                .balance(new BigDecimal("500.00"))
                .status("FROZEN")
                .build();

        Account toAcc = Account.builder()
                .id(2L)
                .accountNumber("200000000002")
                .customer(c2)
                .balance(new BigDecimal("200.00"))
                .status("ACTIVE")
                .build();

        when(accountRepository.findByAccountNumberWithLock("100000000001")).thenReturn(Optional.of(fromAcc));
        when(accountRepository.findByAccountNumberWithLock("200000000002")).thenReturn(Optional.of(toAcc));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("100000000001")
                .toAccountNumber("200000000002")
                .amount(new BigDecimal("50.00"))
                .build();

        assertThrows(InvalidOperationException.class, () -> transferService.transfer(request));

        verify(transferRecordService).recordFailedTransfer(
                any(), eq(fromAcc), eq(toAcc), eq(new BigDecimal("50.00")), any());
    }

    @Test
    @DisplayName("Duplicate idempotencyKey returns existing result without moving money again")
    void transfer_duplicateIdempotencyKey_returnsExisting() {
        Account fromAcc = Account.builder().accountNumber("100000000001").build();
        Account toAcc = Account.builder().accountNumber("200000000002").build();

        TransferRecord existing = TransferRecord.builder()
                .transactionId("existing-tx-id")
                .idempotencyKey("key-12345")
                .fromAccount(fromAcc)
                .toAccount(toAcc)
                .amount(new BigDecimal("75.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(transferRepository.findByIdempotencyKey("key-12345")).thenReturn(Optional.of(existing));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("100000000001")
                .toAccountNumber("200000000002")
                .amount(new BigDecimal("75.00"))
                .idempotencyKey("key-12345")
                .build();

        TransferResponse response = transferService.transfer(request);

        assertNotNull(response);
        assertEquals("existing-tx-id", response.getTransactionId());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());

        // Account locks and balances never touched
        verify(accountRepository, never()).findByAccountNumberWithLock(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Accounts are locked in alphabetical order regardless of sender/recipient direction")
    void transfer_locksAlphabetically() {
        Customer c1 = Customer.builder().id(1L).email("alice@test.com").build();
        Customer c2 = Customer.builder().id(2L).email("bob@test.com").build();

        Account accLow = Account.builder()
                .id(1L)
                .accountNumber("100000000001")
                .customer(c1)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build();

        Account accHigh = Account.builder()
                .id(2L)
                .accountNumber("900000000009")
                .customer(c2)
                .balance(new BigDecimal("500.00"))
                .status("ACTIVE")
                .build();

        when(accountRepository.findByAccountNumberWithLock("100000000001")).thenReturn(Optional.of(accLow));
        when(accountRepository.findByAccountNumberWithLock("900000000009")).thenReturn(Optional.of(accHigh));
        when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Test 1: Sender is accHigh, Recipient is accLow (high -> low)
        TransferRequest req1 = TransferRequest.builder()
                .fromAccountNumber("900000000009")
                .toAccountNumber("100000000001")
                .amount(new BigDecimal("50.00"))
                .build();

        transferService.transfer(req1);

        InOrder inOrder = Mockito.inOrder(accountRepository);
        inOrder.verify(accountRepository).findByAccountNumberWithLock("100000000001");
        inOrder.verify(accountRepository).findByAccountNumberWithLock("900000000009");
    }
}
