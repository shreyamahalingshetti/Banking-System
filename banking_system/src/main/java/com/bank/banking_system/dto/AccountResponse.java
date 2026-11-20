package com.bank.banking_system.dto;

import com.bank.banking_system.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Long customerId;
    private BigDecimal balance;
    private String status;
    private AccountType accountType;
    private LocalDateTime createdAt;
}
