package com.bank.banking_system.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Register returns 201 with CustomerResponse and no password field")
    void register_returns201_withoutPassword() throws Exception {
        String uniqueEmail = "auth_reg_" + System.nanoTime() + "@bank.com";
        String body = String.format("{\"fullName\":\"Test User\",\"email\":\"%s\",\"password\":\"Secret123\"}", uniqueEmail);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertFalse(json.contains("\"password\""), "Register response must not contain password");
        assertFalse(json.contains("\"passwordHash\""), "Register response must not contain passwordHash");
    }

    @Test
    @DisplayName("Duplicate email returns 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        String email = "dup_" + System.nanoTime() + "@bank.com";
        String body = String.format("{\"fullName\":\"User One\",\"email\":\"%s\",\"password\":\"Secret123\"}", email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Invalid input (short password / invalid email) returns 400 Bad Request")
    void register_invalidInput_returns400() throws Exception {
        String invalidBody = "{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"123\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("Login returns a valid JWT token")
    void login_validCredentials_returnsJwt() throws Exception {
        String email = "login_test_" + System.nanoTime() + "@bank.com";
        String regBody = String.format("{\"fullName\":\"Login Test\",\"email\":\"%s\",\"password\":\"Password123\"}", email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regBody))
                .andExpect(status().isCreated());

        String loginBody = String.format("{\"email\":\"%s\",\"password\":\"Password123\"}", email);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertTrue(m.find(), "Response must contain token");
        String token = m.group(1);
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "Returned token must be a standard 3-part JWT");
    }

    @Test
    @DisplayName("Wrong password returns 401 with a generic error message")
    void login_wrongPassword_returns401() throws Exception {
        String email = "wrong_pwd_" + System.nanoTime() + "@bank.com";
        String regBody = String.format("{\"fullName\":\"Wrong Pwd Test\",\"email\":\"%s\",\"password\":\"Correct123\"}", email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regBody))
                .andExpect(status().isCreated());

        String badLoginBody = String.format("{\"email\":\"%s\",\"password\":\"IncorrectPassword\"}", email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
