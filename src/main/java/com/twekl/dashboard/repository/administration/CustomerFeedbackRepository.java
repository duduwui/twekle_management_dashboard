package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.CustomerFeedback;
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

    @Query("SELECT COUNT(f) FROM CustomerFeedback f WHERE f.rating <= 2")
    long countUnsatisfied();

    @Query("SELECT COUNT(f) FROM CustomerFeedback f WHERE f.rating >= 4")
    long countSatisfied();

    @Query("SELECT COUNT(f) FROM CustomerFeedback f WHERE f.rating = 3")
    long countNeutral();

    @Query("SELECT COALESCE(SUM(f.rating), 0) FROM CustomerFeedback f")
    long sumRatings();

    @Query("SELECT f FROM CustomerFeedback f WHERE (:cutoff IS NULL OR f.feedbackDate >= :cutoff) ORDER BY f.feedbackDate DESC")
    List<CustomerFeedback> findRecentFeedbacks(@Param("cutoff") java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COALESCE(AVG(f.rating), 0.0) FROM CustomerFeedback f WHERE (:cutoff IS NULL OR f.feedbackDate >= :cutoff)")
    Double getAverageRating(@Param("cutoff") java.time.Instant cutoff);
}

