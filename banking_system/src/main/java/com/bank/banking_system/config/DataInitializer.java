package com.bank.banking_system.config;

import com.bank.banking_system.entity.Customer;
import com.bank.banking_system.entity.Role;
import com.bank.banking_system.repository.AccountRepository;
import com.bank.banking_system.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@bank.com}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // One-time update to ensure existing database rows without account_type have SAVINGS
        try {
            accountRepository.updateNullAccountTypesToSavings();
            log.info("Verified and updated any null account types to SAVINGS.");
        } catch (Exception e) {
            log.warn("Could not update null account types on startup: {}", e.getMessage());
        }

        if (!customerRepository.existsByEmail(adminEmail)) {
            Customer admin = Customer.builder()
                    .fullName("System Administrator")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();
            customerRepository.save(admin);
            log.info("Initialized default admin user: {}", adminEmail);
        }
    }
}
