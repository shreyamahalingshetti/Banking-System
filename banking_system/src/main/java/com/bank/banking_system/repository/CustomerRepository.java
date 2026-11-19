package com.bank.banking_system.repository;

import com.bank.banking_system.dto.AdminCustomerResponse;
import com.bank.banking_system.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query(value = "SELECT new com.bank.banking_system.dto.AdminCustomerResponse(" +
            "c.id, c.fullName, c.email, c.role, c.createdAt, COUNT(a.id)) " +
            "FROM Customer c LEFT JOIN Account a ON a.customer.id = c.id " +
            "GROUP BY c.id, c.fullName, c.email, c.role, c.createdAt",
            countQuery = "SELECT COUNT(c) FROM Customer c")
    Page<AdminCustomerResponse> findAllWithAccountCount(Pageable pageable);
}
