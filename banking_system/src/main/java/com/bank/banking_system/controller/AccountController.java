package com.bank.banking_system.controller;

import com.bank.banking_system.dto.AccountResponse;
import com.bank.banking_system.dto.CreateAccountRequest;
import com.bank.banking_system.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Accounts", description = "Account creation and retrieval")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Create a new bank account with specified account type")
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Long customerId = (request != null && request.getCustomerId() != null)
                ? request.getCustomerId()
                : accountService.getCurrentCustomerId();
        AccountResponse response = accountService.createAccount(customerId, request.getAccountType());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all accounts belonging to the authenticated customer")
    @GetMapping("/me")
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        List<AccountResponse> responses = accountService.getMyAccounts();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get account details by account number")
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getByAccountNumber(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all accounts for a given customer ID (admin or owner)")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getByCustomerId(@PathVariable Long customerId) {
        List<AccountResponse> responses = accountService.getByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }
}
