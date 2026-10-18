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
    List<Customer> findByLastOrderDateAfter(java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);
    long countByCreatedAtAfter(java.time.Instant cutoff);
    long countByCreatedAtBetween(java.time.Instant from, java.time.Instant to);
    List<Customer> findByLastOrderDateBetween(java.time.Instant from, java.time.Instant to, org.springframework.data.domain.Pageable pageable);

    Optional<Customer> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);

    org.springframework.data.domain.Page<Customer> findByCreatedAtAfter(java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Customer> findByCreatedAtBetween(java.time.Instant from, java.time.Instant to, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false)",
           countQuery = "SELECT count(c) FROM Customer c WHERE EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false)")
    org.springframework.data.domain.Page<Customer> findOverdueCustomers(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE c.createdAt >= :cutoff AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false)",
           countQuery = "SELECT count(c) FROM Customer c WHERE c.createdAt >= :cutoff AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false)")
    org.springframework.data.domain.Page<Customer> findOverdueCustomersWithCutoff(@Param("cutoff") java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE (c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)))",
           countQuery = "SELECT count(c) FROM Customer c WHERE (c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)))")
    org.springframework.data.domain.Page<Customer> findInQueueCustomers(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE c.createdAt >= :cutoff AND (c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)))",
           countQuery = "SELECT count(c) FROM Customer c WHERE c.createdAt >= :cutoff AND (c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)))")
    org.springframework.data.domain.Page<Customer> findInQueueCustomersWithCutoff(@Param("cutoff") java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)",
           countQuery = "SELECT count(c) FROM Customer c WHERE c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)")
    org.springframework.data.domain.Page<Customer> findCompletedCustomers(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE c.createdAt >= :cutoff AND c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)",
           countQuery = "SELECT count(c) FROM Customer c WHERE c.createdAt >= :cutoff AND c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)")
    org.springframework.data.domain.Page<Customer> findCompletedCustomersWithCutoff(@Param("cutoff") java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    org.springframework.data.domain.Page<Customer> searchCustomers(@Param("query") String query, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.createdAt >= :cutoff AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    org.springframework.data.domain.Page<Customer> searchCustomersWithCutoff(@Param("query") String query, @Param("cutoff") java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH o.checks WHERE c.id = :id")
    Optional<Customer> findWithOrdersAndChecksById(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH o.checks ORDER BY c.id DESC")
    List<Customer> findAllWithOrdersAndChecks();

    @Query(value = "SELECT " +
           "SUM(CASE WHEN TOTAL_ORDERS >= 5 THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN TOTAL_ORDERS >= 5 THEN TOTAL_ORDERS ELSE 0 END), 0), " +
           "SUM(CASE WHEN TOTAL_ORDERS >= 3 AND TOTAL_ORDERS < 5 THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN TOTAL_ORDERS >= 3 AND TOTAL_ORDERS < 5 THEN TOTAL_ORDERS ELSE 0 END), 0), " +
           "SUM(CASE WHEN TOTAL_ORDERS = 2 THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN TOTAL_ORDERS = 2 THEN TOTAL_ORDERS ELSE 0 END), 0), " +
           "SUM(CASE WHEN TOTAL_ORDERS <= 1 THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN TOTAL_ORDERS <= 1 THEN TOTAL_ORDERS ELSE 0 END), 0) " +
           "FROM CUSTOMERS", nativeQuery = true)
    List<Object[]> getCustomerTierStats();
}

