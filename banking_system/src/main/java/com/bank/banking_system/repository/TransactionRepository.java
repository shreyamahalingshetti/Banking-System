package com.bank.banking_system.repository;

import com.bank.banking_system.entity.Transaction;
import com.bank.banking_system.entity.TransactionStatus;
import com.bank.banking_system.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    long countByStatus(TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.status = :status")
    BigDecimal sumAmountByTypeAndStatus(@Param("type") TransactionType type, @Param("status") TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt >= :startOfDay")
    long countTransactionsSince(@Param("startOfDay") LocalDateTime startOfDay);

    @Query(value = "SELECT t FROM Transaction t JOIN FETCH t.account a WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:type IS NULL OR t.type = :type) AND " +
            "(:accountNumber IS NULL OR a.accountNumber = :accountNumber) " +
            "ORDER BY t.createdAt DESC",
            countQuery = "SELECT COUNT(t) FROM Transaction t WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:type IS NULL OR t.type = :type) AND " +
            "(:accountNumber IS NULL OR t.account.accountNumber = :accountNumber)")
    Page<Transaction> findWithFilters(@Param("status") TransactionStatus status,
                                      @Param("type") TransactionType type,
                                      @Param("accountNumber") String accountNumber,
                                      Pageable pageable);
}
