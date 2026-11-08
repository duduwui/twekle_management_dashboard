package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    
    Optional<Customer> findByExternalId(String externalId);
    Optional<Customer> findByNameIgnoreCase(String name);
    List<Customer> findByNameContainingIgnoreCase(String name);
    
    List<Customer> findByLastOrderDateAfter(Instant cutoff);
    List<Customer> findByLastOrderDateAfter(Instant cutoff, Pageable pageable);
    long countByCreatedAtAfter(Instant cutoff);
    long countByCreatedAtBetween(Instant from, Instant to);
    List<Customer> findByLastOrderDateBetween(Instant from, Instant to, Pageable pageable);

    Page<Customer> findByCreatedAtAfter(Instant cutoff, Pageable pageable);
    Page<Customer> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))",
           countQuery = "SELECT count(c) FROM Customer c WHERE EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))")
    Page<Customer> findOverdueCustomers(Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE c.createdAt >= :cutoff AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))",
           countQuery = "SELECT count(c) FROM Customer c WHERE c.createdAt >= :cutoff AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))")
    Page<Customer> findOverdueCustomersWithCutoff(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE (c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true)) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))))",
           countQuery = "SELECT count(c) FROM Customer c WHERE (c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true)) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))))")
    Page<Customer> findInQueueCustomers(Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE c.createdAt >= :cutoff AND (c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true)) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))))",
           countQuery = "SELECT count(c) FROM Customer c WHERE c.createdAt >= :cutoff AND (c.totalOrders = 0 OR (NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.status = 'OVERDUE' AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true)) AND EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))))")
    Page<Customer> findInQueueCustomersWithCutoff(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))",
           countQuery = "SELECT count(c) FROM Customer c WHERE c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))")
    Page<Customer> findCompletedCustomers(Pageable pageable);

    @Query(value = "SELECT c FROM Customer c WHERE c.createdAt >= :cutoff AND c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))",
           countQuery = "SELECT count(c) FROM Customer c WHERE c.createdAt >= :cutoff AND c.totalOrders > 0 AND NOT EXISTS (SELECT 1 FROM CustomerOrder o JOIN o.checks ch WHERE o.customer = c AND ch.completed = false AND (ch.preset IS NULL OR ch.preset.active = true))")
    Page<Customer> findCompletedCustomersWithCutoff(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.externalId) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomers(@Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.createdAt >= :cutoff AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.externalId) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomersWithCutoff(@Param("query") String query, @Param("cutoff") Instant cutoff, Pageable pageable);

    @Query(value = "SELECT DISTINCT c FROM Customer c JOIN CustomerOrder o ON o.customer = c JOIN o.checks ch " +
                   "WHERE (:presetId IS NOT NULL AND ch.preset.id = :presetId) OR (:presetName IS NOT NULL AND ch.milestoneName = :presetName)",
           countQuery = "SELECT count(DISTINCT c) FROM Customer c JOIN CustomerOrder o ON o.customer = c JOIN o.checks ch " +
                        "WHERE (:presetId IS NOT NULL AND ch.preset.id = :presetId) OR (:presetName IS NOT NULL AND ch.milestoneName = :presetName)")
    Page<Customer> findCustomersByPreset(@Param("presetId") Long presetId, @Param("presetName") String presetName, Pageable pageable);

    @Query(value = "SELECT DISTINCT c FROM Customer c JOIN CustomerOrder o ON o.customer = c JOIN o.checks ch " +
                   "WHERE ((:presetId IS NOT NULL AND ch.preset.id = :presetId) OR (:presetName IS NOT NULL AND ch.milestoneName = :presetName)) AND ch.status = :status AND ch.completed = false",
           countQuery = "SELECT count(DISTINCT c) FROM Customer c JOIN CustomerOrder o ON o.customer = c JOIN o.checks ch " +
                        "WHERE ((:presetId IS NOT NULL AND ch.preset.id = :presetId) OR (:presetName IS NOT NULL AND ch.milestoneName = :presetName)) AND ch.status = :status AND ch.completed = false")
    Page<Customer> findCustomersByPresetAndStatus(@Param("presetId") Long presetId, @Param("presetName") String presetName, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT DISTINCT c FROM Customer c JOIN CustomerOrder o ON o.customer = c JOIN o.checks ch " +
                   "WHERE ((:presetId IS NOT NULL AND ch.preset.id = :presetId) OR (:presetName IS NOT NULL AND ch.milestoneName = :presetName)) AND ch.completed = :completed",
           countQuery = "SELECT count(DISTINCT c) FROM Customer c JOIN CustomerOrder o ON o.customer = c JOIN o.checks ch " +
                        "WHERE ((:presetId IS NOT NULL AND ch.preset.id = :presetId) OR (:presetName IS NOT NULL AND ch.milestoneName = :presetName)) AND ch.completed = :completed")
    Page<Customer> findCustomersByPresetAndCompleted(@Param("presetId") Long presetId, @Param("presetName") String presetName, @Param("completed") boolean completed, Pageable pageable);

    @Query(value = "SELECT DISTINCT c FROM Customer c JOIN CustomerOrder o ON o.customer = c JOIN o.checks ch " +
                   "WHERE ((:presetId IS NOT NULL AND ch.preset.id = :presetId) OR (:presetName IS NOT NULL AND ch.milestoneName = :presetName)) AND " +
                   "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.externalId) LIKE LOWER(CONCAT('%', :query, '%')))",
           countQuery = "SELECT count(DISTINCT c) FROM Customer c JOIN CustomerOrder o ON o.customer = c JOIN o.checks ch " +
                        "WHERE ((:presetId IS NOT NULL AND ch.preset.id = :presetId) OR (:presetName IS NOT NULL AND ch.milestoneName = :presetName)) AND " +
                        "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.externalId) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomersByPreset(@Param("presetId") Long presetId, @Param("presetName") String presetName, @Param("query") String query, Pageable pageable);

    @Query(value = "SELECT " +
           "SUM(CASE WHEN TOTAL_ORDERS >= 3 AND (LAST_ORDER_DATE IS NULL OR DATEDIFF(NOW(), LAST_ORDER_DATE) <= 90 OR TOTAL_ORDERS >= 4) THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN TOTAL_ORDERS >= 3 AND (LAST_ORDER_DATE IS NULL OR DATEDIFF(NOW(), LAST_ORDER_DATE) <= 90 OR TOTAL_ORDERS >= 4) THEN TOTAL_ORDERS ELSE 0 END), 0), " +
           "SUM(CASE WHEN TOTAL_ORDERS >= 2 AND (LAST_ORDER_DATE IS NULL OR DATEDIFF(NOW(), LAST_ORDER_DATE) <= 270) AND NOT (TOTAL_ORDERS >= 3 AND (LAST_ORDER_DATE IS NULL OR DATEDIFF(NOW(), LAST_ORDER_DATE) <= 90 OR TOTAL_ORDERS >= 4)) THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN TOTAL_ORDERS >= 2 AND (LAST_ORDER_DATE IS NULL OR DATEDIFF(NOW(), LAST_ORDER_DATE) <= 270) AND NOT (TOTAL_ORDERS >= 3 AND (LAST_ORDER_DATE IS NULL OR DATEDIFF(NOW(), LAST_ORDER_DATE) <= 90 OR TOTAL_ORDERS >= 4)) THEN TOTAL_ORDERS ELSE 0 END), 0), " +
           "SUM(CASE WHEN TOTAL_ORDERS <= 1 OR (LAST_ORDER_DATE IS NOT NULL AND DATEDIFF(NOW(), LAST_ORDER_DATE) > 270 AND NOT (TOTAL_ORDERS >= 3 AND (DATEDIFF(NOW(), LAST_ORDER_DATE) <= 90 OR TOTAL_ORDERS >= 4))) THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN TOTAL_ORDERS <= 1 OR (LAST_ORDER_DATE IS NOT NULL AND DATEDIFF(NOW(), LAST_ORDER_DATE) > 270 AND NOT (TOTAL_ORDERS >= 3 AND (DATEDIFF(NOW(), LAST_ORDER_DATE) <= 90 OR TOTAL_ORDERS >= 4))) THEN TOTAL_ORDERS ELSE 0 END), 0) " +
           "FROM CUSTOMERS", nativeQuery = true)
    List<Object[]> getCustomerTierStats();
}
