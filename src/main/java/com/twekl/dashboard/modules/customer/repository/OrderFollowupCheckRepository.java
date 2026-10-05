package com.twekl.dashboard.modules.customer.repository;

import com.twekl.dashboard.modules.customer.entity.OrderFollowupCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderFollowupCheckRepository extends JpaRepository<OrderFollowupCheck, Long> {

    long countByIsCompletedTrue();

    // Sort soonest-to-latest: convert all units to hours for a fair comparison
    @Query("SELECT c FROM OrderFollowupCheck c WHERE c.order.id = :orderId " +
           "ORDER BY CASE c.durationUnit " +
           "  WHEN 'HOURS' THEN c.durationValue " +
           "  WHEN 'DAYS'  THEN c.durationValue * 24 " +
           "  WHEN 'WEEKS' THEN c.durationValue * 168 " +
           "  WHEN 'MONTHS' THEN c.durationValue * 720 " +
           "  ELSE c.durationValue END ASC")
    List<OrderFollowupCheck> findByOrderId(@Param("orderId") Long orderId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM OrderFollowupCheck c WHERE c.presetId = :presetId")
    void deleteByPresetId(@Param("presetId") Long presetId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM OrderFollowupCheck c WHERE LOWER(c.presetName) = LOWER(:presetName)")
    void deleteByPresetName(@Param("presetName") String presetName);
}



