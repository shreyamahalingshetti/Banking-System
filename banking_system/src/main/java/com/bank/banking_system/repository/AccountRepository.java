package com.bank.banking_system.repository;

import com.bank.banking_system.entity.Account;
import com.bank.banking_system.entity.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(Long customerId);

    long countByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a")
    BigDecimal sumTotalBalance();

    @Query(value = "SELECT a FROM Account a JOIN FETCH a.customer",
           countQuery = "SELECT COUNT(a) FROM Account a")
    Page<Account> findAllWithCustomer(Pageable pageable);

    @Query(value = "SELECT a FROM Account a JOIN FETCH a.customer WHERE a.status = :status",
           countQuery = "SELECT COUNT(a) FROM Account a WHERE a.status = :status")
    Page<Account> findByStatusWithCustomer(@Param("status") String status, Pageable pageable);

    @Query(value = "SELECT a FROM Account a JOIN FETCH a.customer " +
                   "WHERE (:status IS NULL OR a.status = :status) " +
                   "AND (:accountType IS NULL OR a.accountType = :accountType)",
           countQuery = "SELECT COUNT(a) FROM Account a " +
                        "WHERE (:status IS NULL OR a.status = :status) " +
                        "AND (:accountType IS NULL OR a.accountType = :accountType)")
    Page<Account> findWithFilters(@Param("status") String status,
                                  @Param("accountType") AccountType accountType,
                                  Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "UPDATE accounts SET account_type = 'SAVINGS' WHERE account_type IS NULL", nativeQuery = true)
    void updateNullAccountTypesToSavings();
}
