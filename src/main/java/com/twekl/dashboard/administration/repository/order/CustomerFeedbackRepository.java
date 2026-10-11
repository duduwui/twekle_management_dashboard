package com.twekl.dashboard.administration.repository.order;

import com.twekl.dashboard.administration.model.order.CustomerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Customer Feedback.
 * Isolated in order domain.
 */
@Repository
public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long> {

    @Query("SELECT f FROM CustomerFeedback f LEFT JOIN FETCH f.customer LEFT JOIN FETCH f.order ORDER BY f.feedbackDate DESC")
    List<CustomerFeedback> findAllWithDetails();

    @Query("SELECT f FROM CustomerFeedback f WHERE f.customer.id = :customerId ORDER BY f.feedbackDate DESC")
    List<CustomerFeedback> findByCustomerIdOrderByFeedbackDateDesc(@Param("customerId") Long customerId);
}

