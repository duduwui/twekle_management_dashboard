package com.twekl.dashboard.repository;

import com.twekl.dashboard.model.OrderFollowupCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderFollowupCheckRepository extends JpaRepository<OrderFollowupCheck, Long> {
    List<OrderFollowupCheck> findByOrderIdOrderByIdAsc(Long orderId);
    void deleteByOrderId(Long orderId);
}
