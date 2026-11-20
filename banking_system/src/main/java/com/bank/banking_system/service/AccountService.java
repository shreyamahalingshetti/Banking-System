package com.bank.banking_system.service;

import com.bank.banking_system.config.SecurityUtils;
import com.bank.banking_system.dto.AccountResponse;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.AccountType;
import com.bank.banking_system.entity.Customer;
import com.bank.banking_system.exception.InvalidOperationException;
import com.bank.banking_system.exception.ResourceNotFoundException;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    @Transactional
    public AccountResponse createAccount(Long customerId) {
        return createAccount(customerId, AccountType.SAVINGS);
    }

    @Transactional
    public AccountResponse createAccount(Long customerId, AccountType accountType) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        String currentEmail = SecurityUtils.getCurrentUserEmail();
        if (!"anonymous".equals(currentEmail) && !SecurityUtils.isCurrentUserAdmin()) {
            if (!customer.getEmail().equalsIgnoreCase(currentEmail)) {
                throw new AccessDeniedException("Cannot create account for another customer");
            }
        }

        long existingCount = accountRepository.countByCustomerId(customer.getId());
        if (existingCount >= 5) {
            throw new InvalidOperationException("Customer cannot have more than 5 accounts");
        }

        AccountType effectiveType = (accountType != null) ? accountType : AccountType.SAVINGS;
        String accountNumber = generateUniqueAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customer(customer)
                .accountType(effectiveType)
                .balance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .status("ACTIVE")
                .build();

        Account savedAccount = accountRepository.save(account);

        String performedBy = currentEmail;
        if ("anonymous".equals(performedBy) && customer.getEmail() != null) {
            performedBy = customer.getEmail();
        }

        auditService.log("ACCOUNT_CREATED", performedBy, "ACCOUNT",
                savedAccount.getAccountNumber(),
                "Account created for customer " + customer.getId() + " (" + savedAccount.getAccountType() + ")");

        return mapToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));
        SecurityUtils.checkAccountOwnership(account);
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        String currentEmail = SecurityUtils.getCurrentUserEmail();
        if (!"anonymous".equals(currentEmail) && !SecurityUtils.isCurrentUserAdmin()) {
            if (!customer.getEmail().equalsIgnoreCase(currentEmail)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this customer");
            }
        }

        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts() {
        String currentEmail = SecurityUtils.getCurrentUserEmail();
        Customer customer = customerRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for email: " + currentEmail));
        return accountRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Long getCurrentCustomerId() {
        String currentEmail = SecurityUtils.getCurrentUserEmail();
        Customer customer = customerRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for email: " + currentEmail));
        return customer.getId();
    }

    private String generateUniqueAccountNumber() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            long number = ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L);
            String candidate = String.valueOf(number);
            if (accountRepository.findByAccountNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new RuntimeException("Failed to generate unique account number after multiple attempts");
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomer().getId())
                .balance(account.getBalance())
                .status(account.getStatus())
                .accountType(account.getAccountType())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
