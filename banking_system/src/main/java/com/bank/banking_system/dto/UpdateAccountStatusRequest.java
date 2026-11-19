package com.bank.banking_system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class UpdateAccountStatusRequest {

    @NotNull(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|FROZEN|CLOSED)$", message = "Status must be one of ACTIVE, FROZEN, CLOSED")
    private String status;
}
