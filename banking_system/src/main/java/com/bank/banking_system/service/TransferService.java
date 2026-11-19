package com.bank.banking_system.service;

import com.bank.banking_system.config.SecurityUtils;
import com.bank.banking_system.dto.TransferRequest;
import com.bank.banking_system.dto.TransferResponse;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.EntryType;
import com.bank.banking_system.entity.LedgerEntry;
import com.bank.banking_system.entity.Transaction;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import com.bank.banking_system.entity.TransferRecord;
import com.bank.banking_system.exception.InsufficientFundsException;
import com.bank.banking_system.exception.InvalidOperationException;
import com.bank.banking_system.exception.ResourceNotFoundException;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.LedgerEntryRepository;
import com.bank.banking_system.repository.TransactionRepository;
import com.bank.banking_system.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final TransferRecordService transferRecordService;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        String fromAccNum = request.getFromAccountNumber();
        String toAccNum = request.getToAccountNumber();

        if (fromAccNum.equals(toAccNum)) {
            throw new InvalidOperationException("Sender and recipient account numbers must be different");
        }

        String trimmedKey = (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty())
                ? request.getIdempotencyKey().trim()
                : null;

        if (trimmedKey != null) {
            Optional<TransferRecord> existing = transferRepository.findByIdempotencyKey(trimmedKey);
            if (existing.isPresent()) {
                return mapToResponse(existing.get());
            }
        }

        // Lock both accounts in a consistent alphabetical order to prevent deadlocks
        boolean fromIsFirst = fromAccNum.compareTo(toAccNum) < 0;
        String firstAccNum = fromIsFirst ? fromAccNum : toAccNum;
        String secondAccNum = fromIsFirst ? toAccNum : fromAccNum;

        Account firstAccount = accountRepository.findByAccountNumberWithLock(firstAccNum)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + firstAccNum));

        Account secondAccount = accountRepository.findByAccountNumberWithLock(secondAccNum)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + secondAccNum));

        Account fromAccount = fromIsFirst ? firstAccount : secondAccount;
        Account toAccount = fromIsFirst ? secondAccount : firstAccount;

        // Verify sender ownership (only sender needs to be owned by current user)
        SecurityUtils.checkAccountOwnership(fromAccount);

        // Check if either account is inactive
        if (!"ACTIVE".equalsIgnoreCase(fromAccount.getStatus())) {
            String reason = "Sender account is not active (status: " + fromAccount.getStatus() + ")";
            transferRecordService.recordFailedTransfer(trimmedKey, fromAccount, toAccount, request.getAmount(), reason);
            throw new InvalidOperationException(reason);
        }

        if (!"ACTIVE".equalsIgnoreCase(toAccount.getStatus())) {
            String reason = "Recipient account is not active (status: " + toAccount.getStatus() + ")";
            transferRecordService.recordFailedTransfer(trimmedKey, fromAccount, toAccount, request.getAmount(), reason);
            throw new InvalidOperationException(reason);
        }

        // Check sender balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            String reason = "Insufficient funds. Available balance: " + fromAccount.getBalance();
            transferRecordService.recordFailedTransfer(trimmedKey, fromAccount, toAccount, request.getAmount(), reason);
            throw new InsufficientFundsException(reason);
        }

        // Apply balances
        BigDecimal amount = request.getAmount();
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(amount);
        BigDecimal newToBalance = toAccount.getBalance().add(amount);

        fromAccount.setBalance(newFromBalance);
        toAccount.setBalance(newToBalance);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String transactionId = UUID.randomUUID().toString();

        // Double-entry ledger entries
        LedgerEntry debitEntry = LedgerEntry.builder()
                .transactionId(transactionId)
                .account(fromAccount)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .balanceAfter(newFromBalance)
                .build();
        ledgerEntryRepository.save(debitEntry);

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transactionId(transactionId)
                .account(toAccount)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .balanceAfter(newToBalance)
                .build();
        ledgerEntryRepository.save(creditEntry);

        // Transaction records for each account history
        Transaction fromTx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .account(fromAccount)
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .balanceAfter(newFromBalance)
                .status(TransactionStatus.SUCCESS)
                .failureReason(null)
                .build();
        transactionRepository.save(fromTx);

        Transaction toTx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .account(toAccount)
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .balanceAfter(newToBalance)
                .status(TransactionStatus.SUCCESS)
                .failureReason(null)
                .build();
        transactionRepository.save(toTx);

        // TransferRecord
        TransferRecord transferRecord = TransferRecord.builder()
                .transactionId(transactionId)
                .idempotencyKey(trimmedKey)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .failureReason(null)
                .build();
        TransferRecord savedTransfer = transferRepository.save(transferRecord);

        return mapToResponse(savedTransfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getTransfers(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        SecurityUtils.checkAccountOwnership(account);

        return transferRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TransferResponse mapToResponse(TransferRecord transfer) {
        return TransferResponse.builder()
                .transactionId(transfer.getTransactionId())
                .fromAccountNumber(transfer.getFromAccount() != null ? transfer.getFromAccount().getAccountNumber() : null)
                .toAccountNumber(transfer.getToAccount() != null ? transfer.getToAccount().getAccountNumber() : null)
                .amount(transfer.getAmount())
                .status(transfer.getStatus())
                .failureReason(transfer.getFailureReason())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
