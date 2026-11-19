package com.bank.banking_system.service;

import com.bank.banking_system.dto.CustomerResponse;
import com.bank.banking_system.dto.LoginRequest;
import com.bank.banking_system.dto.LoginResponse;
import com.bank.banking_system.dto.RegisterRequest;
import com.bank.banking_system.entity.Customer;
import com.bank.banking_system.entity.Role;
import com.bank.banking_system.exception.EmailAlreadyRegisteredException;
import com.bank.banking_system.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public CustomerResponse register(RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException("Email is already registered: " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        auditService.log("REGISTER", savedCustomer.getEmail(), "CUSTOMER",
                savedCustomer.getId().toString(), "Customer registered successfully");

        return CustomerResponse.builder()
                .id(savedCustomer.getId())
                .fullName(savedCustomer.getFullName())
                .email(savedCustomer.getEmail())
                .role(savedCustomer.getRole())
                .createdAt(savedCustomer.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);

        if (customerOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), customerOpt.get().getPassword())) {
            auditService.log("LOGIN_FAILED", email, "AUTH", null, "Failed login attempt: invalid credentials");
            throw new BadCredentialsException("Invalid email or password");
        }

        Customer customer = customerOpt.get();
        String token = jwtService.generateToken(customer.getEmail(), customer.getId(), customer.getRole());

        auditService.log("LOGIN_SUCCESS", customer.getEmail(), "CUSTOMER",
                customer.getId().toString(), "Login successful");

        return LoginResponse.builder()
                .token(token)
                .email(customer.getEmail())
                .role(customer.getRole())
                .build();
    }
}
