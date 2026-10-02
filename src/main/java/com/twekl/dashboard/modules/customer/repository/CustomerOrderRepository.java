package com.twekl.dashboard.modules.customer.repository;

import com.twekl.dashboard.modules.customer.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
}
