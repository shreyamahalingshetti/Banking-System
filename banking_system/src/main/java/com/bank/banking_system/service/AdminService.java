package com.bank.banking_system.service;

import com.bank.banking_system.config.SecurityUtils;
import com.bank.banking_system.dto.AdminAccountResponse;
import com.bank.banking_system.dto.AdminCustomerResponse;
import com.bank.banking_system.dto.AdminTransactionResponse;
import com.bank.banking_system.dto.AuditLogResponse;
import com.bank.banking_system.dto.DashboardSummaryResponse;
import com.bank.banking_system.dto.PagedResponse;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.AccountType;
import com.bank.banking_system.entity.AuditLog;
import com.bank.banking_system.entity.Transaction;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import com.bank.banking_system.exception.InvalidOperationException;
import com.bank.banking_system.exception.ResourceNotFoundException;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.AuditLogRepository;
import com.bank.banking_system.repository.CustomerRepository;
import com.bank.banking_system.repository.TransactionRepository;
import com.bank.banking_system.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalCustomers = customerRepository.count();
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countByStatus("ACTIVE");
        long frozenAccounts = accountRepository.countByStatus("FROZEN");
        long closedAccounts = accountRepository.countByStatus("CLOSED");
        BigDecimal totalBalance = accountRepository.sumTotalBalance();

        long totalTransactions = transactionRepository.count();
        long successfulTransactions = transactionRepository.countByStatus(TransactionStatus.SUCCESS);
        long failedTransactions = transactionRepository.countByStatus(TransactionStatus.FAILED);

        BigDecimal totalDeposited = transactionRepository.sumAmountByTypeAndStatus(TransactionType.DEPOSIT, TransactionStatus.SUCCESS);
        BigDecimal totalWithdrawn = transactionRepository.sumAmountByTypeAndStatus(TransactionType.WITHDRAWAL, TransactionStatus.SUCCESS);
        BigDecimal totalTransferred = transferRepository.sumAmountByStatus(TransactionStatus.SUCCESS);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long transactionsToday = transactionRepository.countTransactionsSince(startOfToday);

        return DashboardSummaryResponse.builder()
                .totalCustomers(totalCustomers)
                .totalAccounts(totalAccounts)
                .activeAccounts(activeAccounts)
                .frozenAccounts(frozenAccounts)
                .closedAccounts(closedAccounts)
                .totalBalanceAcrossAccounts(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulTransactions)
                .failedTransactions(failedTransactions)
                .totalDepositedAmount(totalDeposited != null ? totalDeposited : BigDecimal.ZERO)
                .totalWithdrawnAmount(totalWithdrawn != null ? totalWithdrawn : BigDecimal.ZERO)
                .totalTransferredAmount(totalTransferred != null ? totalTransferred : BigDecimal.ZERO)
                .transactionsToday(transactionsToday)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminCustomerResponse> listCustomers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminCustomerResponse> customerPage = customerRepository.findAllWithAccountCount(pageable);

        return PagedResponse.<AdminCustomerResponse>builder()
                .content(customerPage.getContent())
                .page(customerPage.getNumber())
                .size(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminAccountResponse> listAccounts(int page, int size, String status) {
        return listAccounts(page, size, status, null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminAccountResponse> listAccounts(int page, int size, String status, AccountType type) {
        Pageable pageable = PageRequest.of(page, size);
        String trimmedStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;
        Page<Account> accountPage = accountRepository.findWithFilters(trimmedStatus, type, pageable);

        return PagedResponse.<AdminAccountResponse>builder()
                .content(accountPage.getContent().stream().map(this::mapToAdminAccountResponse).toList())
                .page(accountPage.getNumber())
                .size(accountPage.getSize())
                .totalElements(accountPage.getTotalElements())
                .totalPages(accountPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminTransactionResponse> listTransactions(int page, int size,
                                                                  TransactionStatus status,
                                                                  TransactionType type,
                                                                  String accountNumber) {
        Pageable pageable = PageRequest.of(page, size);
        String accNum = (accountNumber != null && !accountNumber.trim().isEmpty()) ? accountNumber.trim() : null;
        Page<Transaction> txPage = transactionRepository.findWithFilters(status, type, accNum, pageable);

        return PagedResponse.<AdminTransactionResponse>builder()
                .content(txPage.getContent().stream().map(this::mapToAdminTransactionResponse).toList())
                .page(txPage.getNumber())
                .size(txPage.getSize())
                .totalElements(txPage.getTotalElements())
                .totalPages(txPage.getTotalPages())
                .build();
    }

    @Transactional
    public AdminAccountResponse updateAccountStatus(String accountNumber, String newStatus) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        String currentStatus = account.getStatus();
        String targetStatus = newStatus.trim().toUpperCase();

        if ("CLOSED".equalsIgnoreCase(currentStatus) && !"CLOSED".equalsIgnoreCase(targetStatus)) {
            throw new InvalidOperationException("Cannot change status of a CLOSED account");
        }

        account.setStatus(targetStatus);
        Account saved = accountRepository.save(account);

        String performedBy = SecurityUtils.getCurrentUserEmail();
        auditService.log("ACCOUNT_STATUS_CHANGED", performedBy, "ACCOUNT", accountNumber,
                currentStatus + " -> " + targetStatus);

        return mapToAdminAccountResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> listAuditLogs(int page, int size, String action) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logPage;
        if (action != null && !action.trim().isEmpty()) {
            logPage = auditLogRepository.findByActionOrderByCreatedAtDesc(action.trim(), pageable);
        } else {
            logPage = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return PagedResponse.<AuditLogResponse>builder()
                .content(logPage.getContent().stream().map(this::mapToAuditLogResponse).toList())
                .page(logPage.getNumber())
                .size(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .build();
    }

    private AdminAccountResponse mapToAdminAccountResponse(Account account) {
        return AdminAccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomer() != null ? account.getCustomer().getId() : null)
                .customerName(account.getCustomer() != null ? account.getCustomer().getFullName() : null)
                .balance(account.getBalance())
                .status(account.getStatus())
                .accountType(account.getAccountType())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private AdminTransactionResponse mapToAdminTransactionResponse(Transaction tx) {
        return AdminTransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .accountNumber(tx.getAccount() != null ? tx.getAccount().getAccountNumber() : null)
                .type(tx.getType())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .status(tx.getStatus())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private AuditLogResponse mapToAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .performedBy(log.getPerformedBy())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
