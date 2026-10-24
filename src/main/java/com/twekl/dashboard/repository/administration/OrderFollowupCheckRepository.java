package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA Repository for Follow-up Milestone Checks.
 * Dedicated followup domain repository optimized with covering index queries.
 */
@Repository
public interface OrderFollowupCheckRepository extends JpaRepository<OrderFollowupCheck, Long> {

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.preset IS NULL OR c.preset.active = true")
    long countActiveChecks();

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.completed = true AND (c.preset IS NULL OR c.preset.active = true)")
    long countActiveCompletedChecks();

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.completed = false AND c.status = 'OVERDUE' AND (c.preset IS NULL OR c.preset.active = true)")
    long countActiveOverdueChecks();

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE UPPER(c.satisfaction) IN ('SATISFIED', '4', '5') AND (c.preset IS NULL OR c.preset.active = true)")
    long countSatisfied();

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE UPPER(c.satisfaction) IN ('NEUTRAL', '3') AND (c.preset IS NULL OR c.preset.active = true)")
    long countNeutral();

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE UPPER(c.satisfaction) IN ('UNSATISFIED', '1', '2') AND (c.preset IS NULL OR c.preset.active = true)")
    long countUnsatisfied();

    @Query("SELECT c FROM OrderFollowupCheck c JOIN FETCH c.order o LEFT JOIN FETCH c.preset " +
           "WHERE o.customer.id IN :customerIds")
    List<OrderFollowupCheck> findByCustomerIds(@Param("customerIds") List<Long> customerIds);

    @Query("SELECT c FROM OrderFollowupCheck c JOIN FETCH c.order o LEFT JOIN FETCH c.preset " +
           "WHERE o.id IN :orderIds")
    List<OrderFollowupCheck> findByOrderIds(@Param("orderIds") List<Long> orderIds);

    @Query("SELECT c FROM OrderFollowupCheck c LEFT JOIN FETCH c.preset WHERE c.order.id = :orderId " +
           "ORDER BY CASE c.durationUnit " +
           "  WHEN 'HOURS' THEN c.durationValue " +
           "  WHEN 'DAYS'  THEN c.durationValue * 24 " +
           "  WHEN 'WEEKS' THEN c.durationValue * 168 " +
           "  WHEN 'MONTHS' THEN c.durationValue * 720 " +
           "  ELSE c.durationValue END ASC")
    List<OrderFollowupCheck> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT c FROM OrderFollowupCheck c LEFT JOIN FETCH c.order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH c.preset " +
           "WHERE c.satisfaction IS NOT NULL AND TRIM(c.satisfaction) != '' ORDER BY c.id DESC")
    List<OrderFollowupCheck> findChecksWithSatisfaction(Pageable pageable);

    @Query(value = "SELECT p.ID, p.LABEL_EN, p.DISPLAY_ORDER, COUNT(c.ID), " +
           "SUM(CASE WHEN c.COMPLETED = 1 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.COMPLETED = 0 AND c.STATUS = 'OVERDUE' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.COMPLETED = 0 AND (c.STATUS = 'PENDING' OR c.STATUS IS NULL) THEN 1 ELSE 0 END) " +
           "FROM TIME_FILTER_PRESETS p " +
           "LEFT JOIN ORDER_FOLLOWUP_CHECKS c ON c.PRESET_ID = p.ID " +
           "WHERE p.ACTIVE = 1 " +
           "GROUP BY p.ID, p.LABEL_EN, p.DISPLAY_ORDER " +
           "ORDER BY p.DISPLAY_ORDER ASC", nativeQuery = true)
    List<Object[]> getActivePresetCheckBreakdown();

    @Query("SELECT c.milestoneName, COUNT(c.id), " +
           "SUM(CASE WHEN c.completed = true THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN c.completed = false THEN 1L ELSE 0L END) " +
           "FROM OrderFollowupCheck c " +
           "WHERE c.milestoneName IS NOT NULL " +
           "GROUP BY c.milestoneName")
    List<Object[]> getMilestoneStatsAll();

    @Query("SELECT c.milestoneName, COUNT(c.id), " +
           "SUM(CASE WHEN c.completed = true THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN c.completed = false THEN 1L ELSE 0L END) " +
           "FROM OrderFollowupCheck c " +
           "WHERE c.milestoneName IS NOT NULL AND c.createdAt >= :cutoff " +
           "GROUP BY c.milestoneName")
    List<Object[]> getMilestoneStatsByPeriod(@Param("cutoff") Instant cutoff);

    @Query("SELECT c.milestoneName, COUNT(c.id), " +
           "SUM(CASE WHEN c.completed = true THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN c.completed = false THEN 1L ELSE 0L END) " +
           "FROM OrderFollowupCheck c " +
           "WHERE c.milestoneName IS NOT NULL AND c.createdAt >= :startDate AND c.createdAt <= :endDate " +
           "GROUP BY c.milestoneName")
    List<Object[]> getMilestoneStatsByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT o.ITEMS_SUMMARY, " +
           "SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('SATISFIED', '4', '5') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('NEUTRAL', '3') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('UNSATISFIED', '1', '2') THEN 1 ELSE 0 END), " +
           "COUNT(c.ID) " +
           "FROM CUSTOMER_ORDERS o JOIN ORDER_FOLLOWUP_CHECKS c ON o.ID = c.ORDER_ID " +
           "WHERE (:startDate IS NULL OR (o.ORDER_DATE >= :startDate AND o.ORDER_DATE <= :endDate)) " +
           "GROUP BY o.ITEMS_SUMMARY", nativeQuery = true)
    List<Object[]> getProductSatisfactionCountsByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('SATISFIED', '4', '5') THEN 1 ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('NEUTRAL', '3') THEN 1 ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('UNSATISFIED', '1', '2') THEN 1 ELSE 0 END), 0) " +
           "FROM ORDER_FOLLOWUP_CHECKS c " +
           "WHERE (:startDate IS NULL OR (c.CREATED_AT >= :startDate AND c.CREATED_AT <= :endDate))", nativeQuery = true)
    List<Object[]> getSatisfactionCountsCombined(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = "UPDATE ORDER_FOLLOWUP_CHECKS ch JOIN CUSTOMER_ORDERS o ON ch.ORDER_ID = o.ID " +
                   "LEFT JOIN TIME_FILTER_PRESETS p ON ch.PRESET_ID = p.ID " +
                   "SET ch.STATUS = 'OVERDUE' " +
                   "WHERE ch.COMPLETED = 0 AND ch.STATUS != 'OVERDUE' " +
                   "  AND (ch.PRESET_ID IS NULL OR (p.ID IS NOT NULL AND p.ACTIVE = 1)) " +
                   "  AND (CASE " +
                   "        WHEN ch.DURATION_UNIT = 'HOURS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE HOUR) " +
                   "        WHEN ch.DURATION_UNIT = 'DAYS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE DAY) " +
                   "        WHEN ch.DURATION_UNIT = 'WEEKS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL (ch.DURATION_VALUE * 7) DAY) " +
                   "        WHEN ch.DURATION_UNIT = 'MONTHS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE MONTH) " +
                   "        ELSE DATE_ADD(o.ORDER_DATE, INTERVAL ch.TARGET_DAYS DAY) " +
                   "      END) < UTC_TIMESTAMP()", nativeQuery = true)
    int syncOverdueChecks();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = "UPDATE ORDER_FOLLOWUP_CHECKS ch JOIN CUSTOMER_ORDERS o ON ch.ORDER_ID = o.ID " +
                   "LEFT JOIN TIME_FILTER_PRESETS p ON ch.PRESET_ID = p.ID " +
                   "SET ch.STATUS = 'PENDING' " +
                   "WHERE ch.COMPLETED = 0 AND ch.STATUS = 'OVERDUE' " +
                   "  AND ((ch.PRESET_ID IS NOT NULL AND (p.ID IS NULL OR p.ACTIVE = 0)) " +
                   "       OR (CASE " +
                   "        WHEN ch.DURATION_UNIT = 'HOURS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE HOUR) " +
                   "        WHEN ch.DURATION_UNIT = 'DAYS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE DAY) " +
                   "        WHEN ch.DURATION_UNIT = 'WEEKS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL (ch.DURATION_VALUE * 7) DAY) " +
                   "        WHEN ch.DURATION_UNIT = 'MONTHS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE MONTH) " +
                   "        ELSE DATE_ADD(o.ORDER_DATE, INTERVAL ch.TARGET_DAYS DAY) " +
                   "      END) >= UTC_TIMESTAMP())", nativeQuery = true)
    int syncPendingChecks();
}
