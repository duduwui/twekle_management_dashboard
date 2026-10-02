package com.twekl.dashboard.modules.customer.repository;

import com.twekl.dashboard.modules.customer.entity.OrderFollowupCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderFollowupCheckRepository extends JpaRepository<OrderFollowupCheck, Long> {
    List<OrderFollowupCheck> findByOrderId(Long orderId);
}
