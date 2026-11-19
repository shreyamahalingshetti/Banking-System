package com.bank.banking_system.dto;

import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTransactionResponse {

    private String transactionId;
    private String accountNumber;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private TransactionStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
}
