package com.bank.banking_system.controller;

import com.bank.banking_system.dto.AdminAccountResponse;
import com.bank.banking_system.dto.AdminCustomerResponse;
import com.bank.banking_system.dto.AdminTransactionResponse;
import com.bank.banking_system.dto.AuditLogResponse;
import com.bank.banking_system.dto.DashboardSummaryResponse;
import com.bank.banking_system.dto.PagedResponse;
import com.bank.banking_system.dto.UpdateAccountStatusRequest;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import com.bank.banking_system.exception.InvalidOperationException;
import com.bank.banking_system.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "Admin-only dashboard and management endpoints (ROLE_ADMIN required)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Get system-wide dashboard summary statistics")
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminService.getSummary());
    }

    @Operation(summary = "List all customers (paginated)")
    @GetMapping("/customers")
    public ResponseEntity<PagedResponse<AdminCustomerResponse>> listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePagination(page, size);
        int effectiveSize = Math.min(size, 100);
        return ResponseEntity.ok(adminService.listCustomers(page, effectiveSize));
    }

    @Operation(summary = "List all accounts, optionally filtered by status (ACTIVE, FROZEN, CLOSED)")
    @GetMapping("/accounts")
    public ResponseEntity<PagedResponse<AdminAccountResponse>> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        validatePagination(page, size);
        int effectiveSize = Math.min(size, 100);
        return ResponseEntity.ok(adminService.listAccounts(page, effectiveSize, status));
    }

    @Operation(summary = "List all transactions with optional filters (status, type, accountNumber)")
    @GetMapping("/transactions")
    public ResponseEntity<PagedResponse<AdminTransactionResponse>> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String accountNumber) {
        validatePagination(page, size);
        int effectiveSize = Math.min(size, 100);
        return ResponseEntity.ok(adminService.listTransactions(page, effectiveSize, status, type, accountNumber));
    }

    @Operation(summary = "Update the status of an account (ACTIVE, FROZEN, or CLOSED)")
    @PatchMapping("/accounts/{accountNumber}/status")
    public ResponseEntity<AdminAccountResponse> updateAccountStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        AdminAccountResponse response = adminService.updateAccountStatus(accountNumber, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List audit log entries, optionally filtered by action keyword")
    @GetMapping("/audit-logs")
    public ResponseEntity<PagedResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action) {
        validatePagination(page, size);
        int effectiveSize = Math.min(size, 100);
        return ResponseEntity.ok(adminService.listAuditLogs(page, effectiveSize, action));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidOperationException("Page index must not be negative");
        }
        if (size < 1) {
            throw new InvalidOperationException("Page size must be at least 1");
        }
    }
}
