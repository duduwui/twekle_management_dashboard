package com.twekl.dashboard.repository;

import com.twekl.dashboard.model.CustomerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long> {
    List<CustomerFeedback> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
