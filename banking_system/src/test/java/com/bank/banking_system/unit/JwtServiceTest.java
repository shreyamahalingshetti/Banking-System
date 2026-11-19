package com.bank.banking_system.unit;

import com.bank.banking_system.entity.Role;
import com.bank.banking_system.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    @DisplayName("Generated token validates successfully")
    void generateToken_validatesSuccessfully() {
        String token = jwtService.generateToken("user@example.com", 42L, Role.CUSTOMER);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token), "Generated token should be valid");
    }

    @Test
    @DisplayName("Tampered token fails validation")
    void tamperedToken_failsValidation() {
        String token = jwtService.generateToken("user@example.com", 42L, Role.CUSTOMER);
        String[] parts = token.split("\\.");

        // Tamper with payload (modify encoded characters)
        String tamperedPayload = parts[1] + "X";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];
        assertFalse(jwtService.validateToken(tamperedToken), "Tampered payload token must fail validation");

        // Tamper with signature
        String tamperedSignature = parts[2].substring(0, parts[2].length() - 2) + "==";
        String tamperedSigToken = parts[0] + "." + parts[1] + "." + tamperedSignature;
        assertFalse(jwtService.validateToken(tamperedSigToken), "Tampered signature token must fail validation");
    }

    @Test
    @DisplayName("Expired token fails validation")
    void expiredToken_failsValidation() {
        // Generate a token that expired 60 seconds ago
        String expiredToken = jwtService.generateToken("user@example.com", 42L, Role.CUSTOMER, -60L);

        assertNotNull(expiredToken);
        assertFalse(jwtService.validateToken(expiredToken), "Expired token must fail validation");
    }

    @Test
    @DisplayName("Email, customerId, and role are extracted correctly from token")
    void extraction_extractsEmailCustomerIdAndRole() {
        String token = jwtService.generateToken("admin@bank.com", 99L, Role.ADMIN);

        assertEquals("admin@bank.com", jwtService.extractEmail(token));
        assertEquals(99L, jwtService.extractCustomerId(token));
        assertEquals(Role.ADMIN, jwtService.extractRole(token));
    }
}
