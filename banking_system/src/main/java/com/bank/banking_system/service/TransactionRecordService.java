package com.bank.banking_system.service;

import com.bank.banking_system.config.SecurityUtils;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.Transaction;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import com.bank.banking_system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionRecordService {

    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction recordFailedTransaction(Account account, TransactionType type, BigDecimal amount, String failureReason) {
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .account(account)
                .type(type)
                .amount(amount)
                .balanceAfter(null)
                .status(TransactionStatus.FAILED)
                .failureReason(failureReason)
                .build();
        Transaction saved = transactionRepository.save(transaction);

        String performedBy = SecurityUtils.getCurrentUserEmail();
        if ("anonymous".equals(performedBy) && account.getCustomer() != null) {
            performedBy = account.getCustomer().getEmail();
        }
        String action = (type == TransactionType.WITHDRAWAL) ? "WITHDRAWAL_FAILED"
                : (type == TransactionType.DEPOSIT ? "DEPOSIT_FAILED" : "TRANSACTION_FAILED");
        auditService.log(action, performedBy, "ACCOUNT", account.getAccountNumber(), failureReason);

        return saved;
    }
}
