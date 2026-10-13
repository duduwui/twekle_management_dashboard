package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.OrderFollowupCheck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Follow-up Milestone Checks. Dedicated followup domain repository.
 */
@Repository
public interface OrderFollowupCheckRepository extends JpaRepository<OrderFollowupCheck, Long> {

    @Query("SELECT c.imageUrl FROM OrderFollowupCheck c WHERE c.id = :checkId")
    java.util.Optional<String> findImage(@Param("checkId") Long checkId);

    @Query(
            "SELECT c FROM OrderFollowupCheck c LEFT JOIN FETCH c.preset WHERE c.order.id ="
                + " :orderId ORDER BY CASE c.durationUnit   WHEN 'HOURS' THEN c.durationValue  "
                + " WHEN 'DAYS'  THEN c.durationValue * 24   WHEN 'WEEKS' THEN c.durationValue *"
                + " 168   WHEN 'MONTHS' THEN c.durationValue * 720   ELSE c.durationValue END ASC")
    List<OrderFollowupCheck> findByOrderId(@Param("orderId") Long orderId);
}
