package com.bank.banking_system.service;

import com.bank.banking_system.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret:test-secret-key-for-unit-testing-purposes-only-32b}")
    private String jwtSecret = "test-secret-key-for-unit-testing-purposes-only-32b";

    private static final long EXPIRATION_SECONDS = 86400; // 24 hours

    private static final Pattern SUB_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("\"customerId\"\\s*:\\s*(\\d+)");
    private static final Pattern ROLE_PATTERN = Pattern.compile("\"role\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(-?\\d+)");

    public String generateToken(String email, Role role) {
        return generateToken(email, null, role, EXPIRATION_SECONDS);
    }

    public String generateToken(String email, Long customerId, Role role) {
        return generateToken(email, customerId, role, EXPIRATION_SECONDS);
    }

    public String generateToken(String email, Long customerId, Role role, long expirationSeconds) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + expirationSeconds;

            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String customerIdStr = (customerId != null) ? customerId.toString() : "null";
            String payloadJson = String.format("{\"sub\":\"%s\",\"customerId\":%s,\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
                    email, customerIdStr, role.name(), now, exp);

            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String content = encodedHeader + "." + encodedPayload;
            String signature = sign(content, jwtSecret);

            return content + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate JWT token", e);
            throw new RuntimeException("Could not generate JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            if (token == null) {
                return false;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String content = parts[0] + "." + parts[1];
            String expectedSig = sign(content, jwtSecret);
            if (!expectedSig.equals(parts[2])) {
                return false;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);

            Matcher matcher = EXP_PATTERN.matcher(payload);
            if (!matcher.find()) {
                return false;
            }

            long exp = Long.parseLong(matcher.group(1));
            long now = Instant.now().getEpochSecond();
            return now < exp;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);

            Matcher matcher = SUB_PATTERN.matcher(payload);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Long extractCustomerId(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);

            Matcher matcher = CUSTOMER_ID_PATTERN.matcher(payload);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Role extractRole(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);

            Matcher matcher = ROLE_PATTERN.matcher(payload);
            if (matcher.find()) {
                return Role.valueOf(matcher.group(1));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}
