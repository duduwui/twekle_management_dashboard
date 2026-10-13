package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    
    List<Customer> findByLastOrderDateAfter(java.time.Instant cutoff);
    long countByCreatedAtAfter(java.time.Instant cutoff);

    Optional<Customer> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH o.checks WHERE c.id = :id")
    Optional<Customer> findWithOrdersAndChecksById(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH o.checks ORDER BY c.id DESC")
    List<Customer> findAllWithOrdersAndChecks();
}

