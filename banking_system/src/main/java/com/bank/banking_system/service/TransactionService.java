package com.bank.banking_system.service;

import com.bank.banking_system.config.SecurityUtils;
import com.bank.banking_system.dto.TransactionResponse;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.Transaction;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import com.bank.banking_system.exception.InsufficientFundsException;
import com.bank.banking_system.exception.InvalidOperationException;
import com.bank.banking_system.exception.ResourceNotFoundException;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionRecordService transactionRecordService;

    @Transactional
    public TransactionResponse deposit(String accountNumber, BigDecimal amount) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        SecurityUtils.checkAccountOwnership(account);

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            String reason = "Account is not active (status: " + account.getStatus() + ")";
            transactionRecordService.recordFailedTransaction(account, TransactionType.DEPOSIT, amount, reason);
            throw new InvalidOperationException(reason);
        }

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .account(account)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(newBalance)
                .status(TransactionStatus.SUCCESS)
                .failureReason(null)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Transactional
    public TransactionResponse withdraw(String accountNumber, BigDecimal amount) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        SecurityUtils.checkAccountOwnership(account);

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            String reason = "Account is not active (status: " + account.getStatus() + ")";
            transactionRecordService.recordFailedTransaction(account, TransactionType.WITHDRAWAL, amount, reason);
            throw new InvalidOperationException(reason);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            String reason = "Insufficient funds. Available balance: " + account.getBalance();
            transactionRecordService.recordFailedTransaction(account, TransactionType.WITHDRAWAL, amount, reason);
            throw new InsufficientFundsException(reason);
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .account(account)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceAfter(newBalance)
                .status(TransactionStatus.SUCCESS)
                .failureReason(null)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        SecurityUtils.checkAccountOwnership(account);

        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .accountNumber(transaction.getAccount().getAccountNumber())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .status(transaction.getStatus())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
