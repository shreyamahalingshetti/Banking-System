package com.bank.banking_system.service;

import com.bank.banking_system.config.SecurityUtils;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.Transaction;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import com.bank.banking_system.entity.TransferRecord;
import com.bank.banking_system.repository.TransactionRepository;
import com.bank.banking_system.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferRecordService {

    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferRecord recordFailedTransfer(String idempotencyKey, Account fromAccount, Account toAccount, BigDecimal amount, String failureReason) {
        String txId = UUID.randomUUID().toString();
        TransferRecord transfer = TransferRecord.builder()
                .transactionId(txId)
                .idempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransactionStatus.FAILED)
                .failureReason(failureReason)
                .build();
        TransferRecord saved = transferRepository.save(transfer);

        if (fromAccount != null) {
            Transaction failedTx = Transaction.builder()
                    .transactionId(txId)
                    .account(fromAccount)
                    .type(TransactionType.TRANSFER)
                    .amount(amount)
                    .balanceAfter(null)
                    .status(TransactionStatus.FAILED)
                    .failureReason(failureReason)
                    .build();
            transactionRepository.save(failedTx);
        }

        String performedBy = SecurityUtils.getCurrentUserEmail();
        if ("anonymous".equals(performedBy) && fromAccount != null && fromAccount.getCustomer() != null) {
            performedBy = fromAccount.getCustomer().getEmail();
        }

        String target = ((fromAccount != null) ? fromAccount.getAccountNumber() : "UNKNOWN") +
                " -> " + ((toAccount != null) ? toAccount.getAccountNumber() : "UNKNOWN");
        auditService.log("TRANSFER_FAILED", performedBy, "TRANSFER", target, failureReason);

        return saved;
    }
}
