package com.bank.banking_system.unit;

import com.bank.banking_system.dto.AccountResponse;
import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.Customer;
import com.bank.banking_system.exception.ResourceNotFoundException;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.CustomerRepository;
import com.bank.banking_system.service.AccountService;
import com.bank.banking_system.service.AuditService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("Created account number is exactly 12 digits")
    void createAccount_numberIs12Digits() {
        Customer customer = Customer.builder().id(1L).email("alice@test.com").build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            acc.setId(10L);
            acc.setCreatedAt(LocalDateTime.now());
            return acc;
        });

        AccountResponse response = accountService.createAccount(1L);

        assertNotNull(response);
        assertNotNull(response.getAccountNumber());
        assertEquals(12, response.getAccountNumber().length(), "Account number must be exactly 12 characters");
        assertTrue(response.getAccountNumber().matches("^\\d{12}$"), "Account number must contain only digits");
        assertEquals(new BigDecimal("0.00"), response.getBalance());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    @DisplayName("Retry occurs when a generated account number already exists in repository")
    void createAccount_retryWhenCollision() {
        Customer customer = Customer.builder().id(1L).email("alice@test.com").build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // First attempt collides, second attempt succeeds
        Account existingAcc = Account.builder().id(99L).accountNumber("123456789012").build();
        when(accountRepository.findByAccountNumber(anyString()))
                .thenReturn(Optional.of(existingAcc))
                .thenReturn(Optional.empty());

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            acc.setId(11L);
            acc.setCreatedAt(LocalDateTime.now());
            return acc;
        });

        AccountResponse response = accountService.createAccount(1L);

        assertNotNull(response);
        assertEquals(12, response.getAccountNumber().length());
        // Verify findByAccountNumber was called at least twice due to collision
        verify(accountRepository, atLeast(2)).findByAccountNumber(anyString());
    }

    @Test
    @DisplayName("Creating account for non-existent customer throws ResourceNotFoundException")
    void createAccount_nonExistentCustomer_throwsNotFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.createAccount(999L));
        verify(accountRepository, never()).save(any(Account.class));
    }
}
