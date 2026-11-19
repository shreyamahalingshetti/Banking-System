package com.bank.banking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private long totalCustomers;
    private long totalAccounts;
    private long activeAccounts;
    private long frozenAccounts;
    private long closedAccounts;
    private BigDecimal totalBalanceAcrossAccounts;
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private BigDecimal totalDepositedAmount;
    private BigDecimal totalWithdrawnAmount;
    private BigDecimal totalTransferredAmount;
    private long transactionsToday;
}
