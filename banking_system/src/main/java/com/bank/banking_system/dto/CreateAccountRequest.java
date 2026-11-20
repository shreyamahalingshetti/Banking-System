package com.bank.banking_system.dto;

import com.bank.banking_system.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    private Long customerId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;
}
