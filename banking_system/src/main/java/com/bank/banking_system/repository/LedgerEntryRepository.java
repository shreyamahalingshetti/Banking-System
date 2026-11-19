package com.bank.banking_system.repository;

import com.bank.banking_system.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionId(String transactionId);
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
