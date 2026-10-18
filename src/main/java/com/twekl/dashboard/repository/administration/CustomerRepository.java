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

    Optional<Customer> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);

    org.springframework.data.domain.Page<Customer> findByCreatedAtAfter(java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:cutoff IS NULL OR c.createdAt >= :cutoff)")
    org.springframework.data.domain.Page<Customer> searchCustomers(
            @Param("query") String query,
            @Param("cutoff") java.time.Instant cutoff,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:cutoff IS NULL OR c.createdAt >= :cutoff) AND " +
           "EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false)")
    org.springframework.data.domain.Page<Customer> searchOverdueCustomers(
            @Param("query") String query,
            @Param("cutoff") java.time.Instant cutoff,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:cutoff IS NULL OR c.createdAt >= :cutoff) AND " +
           "(c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)))")
    org.springframework.data.domain.Page<Customer> searchInQueueCustomers(
            @Param("query") String query,
            @Param("cutoff") java.time.Instant cutoff,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:cutoff IS NULL OR c.createdAt >= :cutoff) AND " +
           "c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false)")
    org.springframework.data.domain.Page<Customer> searchCompletedCustomers(
            @Param("query") String query,
            @Param("cutoff") java.time.Instant cutoff,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH o.checks WHERE c.id = :id")
    Optional<Customer> findWithOrdersAndChecksById(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders o LEFT JOIN FETCH o.checks ORDER BY c.id DESC")
    List<Customer> findAllWithOrdersAndChecks();
}

