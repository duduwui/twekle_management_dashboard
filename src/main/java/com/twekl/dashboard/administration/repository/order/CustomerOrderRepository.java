package com.twekl.dashboard.administration.repository.order;

import com.twekl.dashboard.administration.model.order.CustomerOrder;
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

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM CustomerOrder o WHERE o.customer.id = :customerId ORDER BY o.orderDate DESC")
    List<CustomerOrder> findByCustomerIdOrderByOrderDateDesc(@Param("customerId") Long customerId);

    @Query("SELECT o FROM CustomerOrder o LEFT JOIN FETCH o.checks WHERE o.id = :orderId")
    Optional<CustomerOrder> findWithChecksById(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.checks ORDER BY o.orderDate DESC")
    List<CustomerOrder> findAllWithCustomerAndChecks();
}

