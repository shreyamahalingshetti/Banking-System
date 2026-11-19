package com.bank.banking_system.controller;

import com.bank.banking_system.dto.TransferRequest;
import com.bank.banking_system.dto.TransferResponse;
import com.bank.banking_system.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Transfers", description = "Atomic fund transfers with double-entry ledger")
@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "Transfer funds between two accounts (idempotent)")
    @PostMapping("/api/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all transfers involving an account, newest first")
    @GetMapping("/api/accounts/{accountNumber}/transfers")
    public ResponseEntity<List<TransferResponse>> getTransfers(@PathVariable String accountNumber) {
        List<TransferResponse> responses = transferService.getTransfers(accountNumber);
        return ResponseEntity.ok(responses);
    }
}
