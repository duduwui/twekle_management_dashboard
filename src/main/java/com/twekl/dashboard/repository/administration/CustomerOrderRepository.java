package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.CustomerOrder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Customer Orders. Isolated in order domain with explicit orderId
 * and customerId queries.
 */
@Repository
public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

    @Query("SELECT o FROM CustomerOrder o JOIN FETCH o.customer WHERE o.id = :orderId")
    Optional<CustomerOrder> findWithCustomerById(@Param("orderId") Long orderId);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.id = :orderId")
    Optional<CustomerOrder> lockById(@Param("orderId") Long orderId);
}
