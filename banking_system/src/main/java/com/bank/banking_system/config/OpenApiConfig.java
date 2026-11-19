package com.bank.banking_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise Banking System API")
                        .version("1.0.0")
                        .description("""
                                REST API for the Enterprise Banking System.
                                Supports account management, deposits, withdrawals, \
                                fund transfers with double-entry ledger, and admin operations.
                                
                                **Authentication**: Use POST /api/auth/login to obtain a JWT token, \
                                then click **Authorize** and enter `Bearer <token>`.
                                """)
                        .contact(new Contact().name("Banking System").email("admin@bank.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter the JWT token obtained from POST /api/auth/login")));
    }
}
