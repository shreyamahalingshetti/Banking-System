package com.bank.banking_system.repository;

import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<TransferRecord, Long> {

    Optional<TransferRecord> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM TransferRecord t WHERE t.fromAccount.accountNumber = :accountNumber OR t.toAccount.accountNumber = :accountNumber ORDER BY t.createdAt DESC")
    List<TransferRecord> findByAccountNumberOrderByCreatedAtDesc(@Param("accountNumber") String accountNumber);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransferRecord t WHERE t.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") TransactionStatus status);
}
