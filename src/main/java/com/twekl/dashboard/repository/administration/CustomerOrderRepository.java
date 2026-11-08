package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Customer Orders.
 * Optimized with dedicated customerId and date range covering indices.
 */
@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM CustomerOrder o JOIN FETCH o.customer WHERE o.id = :id")
    Optional<CustomerOrder> findByIdWithCustomer(@Param("id") Long id);

    @Query("SELECT o FROM CustomerOrder o JOIN FETCH o.customer WHERE o.orderNumber = :orderNumber ORDER BY o.id DESC")
    List<CustomerOrder> findByOrderNumberWithCustomer(@Param("orderNumber") String orderNumber);

    @Query("SELECT o FROM CustomerOrder o WHERE o.orderNumber = :orderNumber AND o.customer.id = :customerId")
    Optional<CustomerOrder> findByOrderNumberAndCustomerId(@Param("orderNumber") String orderNumber, @Param("customerId") Long customerId);

    @Query("SELECT o FROM CustomerOrder o WHERE o.customer.id IN :customerIds ORDER BY o.orderDate DESC, o.id DESC")
    List<CustomerOrder> findByCustomerIds(@Param("customerIds") List<Long> customerIds);

    @Query("SELECT o FROM CustomerOrder o JOIN FETCH o.customer WHERE o.customer.id = :customerId ORDER BY o.orderDate DESC")
    List<CustomerOrder> findByCustomerIdOrderByOrderDateDesc(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM CustomerOrder o")
    Double sumTotalRevenue();

    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.orderDate >= :cutoff")
    long countByOrderDateAfter(@Param("cutoff") Instant cutoff);

    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.orderDate >= :startDate AND o.orderDate <= :endDate")
    long countByOrderDateBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT o.ITEMS_SUMMARY, COUNT(o.ID) as ord_cnt " +
           "FROM CUSTOMER_ORDERS o " +
           "WHERE (:startDate IS NULL OR (o.ORDER_DATE >= :startDate AND o.ORDER_DATE <= :endDate)) " +
           "GROUP BY o.ITEMS_SUMMARY ORDER BY ord_cnt DESC", nativeQuery = true)
    List<Object[]> getProductFinancialStatsByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
