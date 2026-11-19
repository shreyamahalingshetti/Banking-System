package com.bank.banking_system.controller;

import com.bank.banking_system.dto.AmountRequest;
import com.bank.banking_system.dto.TransactionResponse;
import com.bank.banking_system.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Transactions", description = "Deposit, withdraw and transaction history")
@RestController
@RequestMapping("/api/accounts/{accountNumber}")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Deposit funds into an account")
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody AmountRequest request) {
        TransactionResponse response = transactionService.deposit(accountNumber, request.getAmount());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Withdraw funds from an account")
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable String accountNumber,
            @Valid @RequestBody AmountRequest request) {
        TransactionResponse response = transactionService.withdraw(accountNumber, request.getAmount());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all transactions for an account, newest first")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable String accountNumber) {
        List<TransactionResponse> responses = transactionService.getTransactions(accountNumber);
        return ResponseEntity.ok(responses);
    }
}
