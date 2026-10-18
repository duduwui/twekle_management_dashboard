package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Customer Orders.
 * Isolated in order domain with explicit orderId and customerId queries.
 */
@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

    @Query("SELECT o FROM CustomerOrder o WHERE o.customer.id IN :customerIds ORDER BY o.orderDate DESC, o.id DESC")
    List<CustomerOrder> findByCustomerIds(@Param("customerIds") List<Long> customerIds);

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM CustomerOrder o WHERE o.customer.id = :customerId ORDER BY o.orderDate DESC")
    List<CustomerOrder> findByCustomerIdOrderByOrderDateDesc(@Param("customerId") Long customerId);

    @Query("SELECT o FROM CustomerOrder o LEFT JOIN FETCH o.checks WHERE o.id = :orderId")
    Optional<CustomerOrder> findWithChecksById(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.checks ORDER BY o.orderDate DESC")
    List<CustomerOrder> findAllWithCustomerAndChecks();

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.checks ORDER BY o.orderDate DESC")
    List<CustomerOrder> findRecentOrdersWithCustomerAndChecks(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT o FROM CustomerOrder o JOIN FETCH o.customer WHERE (:cutoff IS NULL OR o.orderDate >= :cutoff) ORDER BY o.orderDate DESC, o.id DESC")
    List<CustomerOrder> findRecentOrdersWithCustomer(@Param("cutoff") java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM CustomerOrder o")
    Double sumTotalRevenue();

    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.orderDate >= :cutoff")
    long countByOrderDateAfter(@Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM CustomerOrder o WHERE o.orderDate >= :cutoff")
    Double sumRevenueByPeriod(@Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.orderDate >= :startDate AND o.orderDate <= :endDate")
    long countByOrderDateBetween(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM CustomerOrder o WHERE o.orderDate >= :startDate AND o.orderDate <= :endDate")
    Double sumRevenueByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query("SELECT o FROM CustomerOrder o JOIN FETCH o.customer WHERE o.orderDate >= :startDate AND o.orderDate <= :endDate ORDER BY o.orderDate DESC, o.id DESC")
    List<CustomerOrder> findRecentOrdersWithCustomerByDateRange(@Param("startDate") java.time.Instant startDate,
                                                               @Param("endDate") java.time.Instant endDate,
                                                               org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT o.ITEMS_SUMMARY, COUNT(o.ID) as ord_cnt " +
           "FROM CUSTOMER_ORDERS o " +
           "WHERE (:startDate IS NULL OR (o.ORDER_DATE >= :startDate AND o.ORDER_DATE <= :endDate)) " +
           "GROUP BY o.ITEMS_SUMMARY ORDER BY ord_cnt DESC", nativeQuery = true)
    List<Object[]> getProductFinancialStatsByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query(value = "SELECT COUNT(o.ID), COALESCE(SUM(o.TOTAL_AMOUNT), 0) " +
           "FROM CUSTOMER_ORDERS o JOIN CUSTOMERS c ON o.CUSTOMER_ID = c.ID " +
           "WHERE c.TOTAL_ORDERS > 1 AND (:startDate IS NULL OR (o.ORDER_DATE >= :startDate AND o.ORDER_DATE <= :endDate))", nativeQuery = true)
    List<Object[]> getRepeatOrderStatsByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);
}

