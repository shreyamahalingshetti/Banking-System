package com.bank.banking_system.unit;

import com.bank.banking_system.dto.CustomerResponse;
import com.bank.banking_system.dto.RegisterRequest;
import com.bank.banking_system.entity.Customer;
import com.bank.banking_system.entity.Role;
import com.bank.banking_system.exception.EmailAlreadyRegisteredException;
import com.bank.banking_system.repository.CustomerRepository;
import com.bank.banking_system.service.AuditService;
import com.bank.banking_system.service.CustomerService;
import com.bank.banking_system.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("Register succeeds and hashes the password, and response never contains the password")
    void register_success() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Alice Wonderland")
                .email("alice@example.com")
                .password("SecretPassword123")
                .build();

        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecretPassword123")).thenReturn("$2a$10$hashedPasswordHere");

        Customer savedCustomer = Customer.builder()
                .id(1L)
                .fullName("Alice Wonderland")
                .email("alice@example.com")
                .password("$2a$10$hashedPasswordHere")
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponse response = customerService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Alice Wonderland", response.getFullName());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertNotNull(response.getCreatedAt());

        // Verify password hashing was called
        verify(passwordEncoder).encode("SecretPassword123");
        verify(customerRepository).save(any(Customer.class));
        verify(auditService).log(anyString(), anyString(), anyString(), anyString(), anyString());

        // Verify CustomerResponse does not expose password
        // CustomerResponse class has no getPassword method; reflection verifies field absence
        boolean hasPasswordField = false;
        for (var field : CustomerResponse.class.getDeclaredFields()) {
            if ("password".equalsIgnoreCase(field.getName())) {
                hasPasswordField = true;
                break;
            }
        }
        assertEquals(false, hasPasswordField, "CustomerResponse must never contain a password field");
    }

    @Test
    @DisplayName("Register with duplicate email throws EmailAlreadyRegisteredException")
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Bob Builder")
                .email("bob@example.com")
                .password("Password123")
                .build();

        when(customerRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> customerService.register(request));

        verify(customerRepository, never()).save(any(Customer.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
