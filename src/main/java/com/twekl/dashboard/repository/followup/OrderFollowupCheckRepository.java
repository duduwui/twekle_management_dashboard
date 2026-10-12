package com.twekl.dashboard.repository.followup;

import com.twekl.dashboard.model.order.OrderFollowupCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Data JPA Repository for Follow-up Milestone Checks.
 * Dedicated followup domain repository.
 */
@Repository
public interface OrderFollowupCheckRepository extends JpaRepository<OrderFollowupCheck, Long> {

    long countByCompletedTrue();

    @Query("SELECT c FROM OrderFollowupCheck c LEFT JOIN FETCH c.preset WHERE c.order.id = :orderId " +
           "ORDER BY CASE c.durationUnit " +
           "  WHEN 'HOURS' THEN c.durationValue " +
           "  WHEN 'DAYS'  THEN c.durationValue * 24 " +
           "  WHEN 'WEEKS' THEN c.durationValue * 168 " +
           "  WHEN 'MONTHS' THEN c.durationValue * 720 " +
           "  ELSE c.durationValue END ASC")
    List<OrderFollowupCheck> findByOrderId(@Param("orderId") Long orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM OrderFollowupCheck c WHERE c.preset.id = :presetId")
    void deleteByPresetId(@Param("presetId") Long presetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM OrderFollowupCheck c WHERE LOWER(c.milestoneName) = LOWER(:presetName)")
    void deleteByPresetName(@Param("presetName") String presetName);

    @Query("SELECT DISTINCT c FROM OrderFollowupCheck c LEFT JOIN FETCH c.order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH c.preset ORDER BY c.id ASC")
    List<OrderFollowupCheck> findAllWithOrderCustomerAndPreset();
}
