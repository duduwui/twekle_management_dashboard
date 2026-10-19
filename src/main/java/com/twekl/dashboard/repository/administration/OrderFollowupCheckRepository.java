package com.twekl.dashboard.repository.administration;

import com.twekl.dashboard.model.administration.OrderFollowupCheck;
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

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE UPPER(c.satisfaction) IN ('SATISFIED', '4', '5')")
    long countSatisfied();

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE UPPER(c.satisfaction) IN ('NEUTRAL', '3')")
    long countNeutral();

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE UPPER(c.satisfaction) IN ('UNSATISFIED', '1', '2')")
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM OrderFollowupCheck c WHERE c.preset.id = :presetId")
    void deleteByPresetId(@Param("presetId") Long presetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM OrderFollowupCheck c WHERE LOWER(c.milestoneName) = LOWER(:presetName)")
    void deleteByPresetName(@Param("presetName") String presetName);

    @Query("SELECT DISTINCT c FROM OrderFollowupCheck c LEFT JOIN FETCH c.order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH c.preset " +
           "WHERE c.satisfaction IS NOT NULL AND TRIM(c.satisfaction) != '' ORDER BY c.id DESC")
    List<OrderFollowupCheck> findChecksWithSatisfaction(org.springframework.data.domain.Pageable pageable);

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
    List<Object[]> getMilestoneStatsByPeriod(@Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :cutoff AND c.completed = true")
    long countCompletedByPeriod(@Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :cutoff")
    long countTotalByPeriod(@Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :cutoff AND UPPER(c.satisfaction) IN ('SATISFIED', '4', '5')")
    long countSatisfiedByPeriod(@Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :cutoff AND UPPER(c.satisfaction) IN ('NEUTRAL', '3')")
    long countNeutralByPeriod(@Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :cutoff AND UPPER(c.satisfaction) IN ('UNSATISFIED', '1', '2')")
    long countUnsatisfiedByPeriod(@Param("cutoff") java.time.Instant cutoff);

    @Query("SELECT c.milestoneName, COUNT(c.id), " +
           "SUM(CASE WHEN c.completed = true THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN c.completed = false THEN 1L ELSE 0L END) " +
           "FROM OrderFollowupCheck c " +
           "WHERE c.milestoneName IS NOT NULL AND c.createdAt >= :startDate AND c.createdAt <= :endDate " +
           "GROUP BY c.milestoneName")
    List<Object[]> getMilestoneStatsByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate AND c.completed = true")
    long countCompletedByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate")
    long countTotalByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate AND UPPER(c.satisfaction) IN ('SATISFIED', '4', '5')")
    long countSatisfiedByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate AND UPPER(c.satisfaction) IN ('NEUTRAL', '3')")
    long countNeutralByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query("SELECT COUNT(c) FROM OrderFollowupCheck c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate AND UPPER(c.satisfaction) IN ('UNSATISFIED', '1', '2')")
    long countUnsatisfiedByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query("SELECT c FROM OrderFollowupCheck c LEFT JOIN FETCH c.order o LEFT JOIN FETCH o.customer cust LEFT JOIN FETCH c.preset " +
           "WHERE c.completed = false AND (:cutoff IS NULL OR c.createdAt >= :cutoff) " +
           "ORDER BY c.id DESC")
    List<OrderFollowupCheck> findPendingChecks(@Param("cutoff") java.time.Instant cutoff, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM OrderFollowupCheck c LEFT JOIN FETCH c.order o LEFT JOIN FETCH o.customer cust LEFT JOIN FETCH c.preset " +
           "WHERE c.completed = false AND c.createdAt >= :startDate AND c.createdAt <= :endDate " +
           "ORDER BY c.id DESC")
    List<OrderFollowupCheck> findPendingChecksByDateRange(@Param("startDate") java.time.Instant startDate,
                                                         @Param("endDate") java.time.Instant endDate,
                                                         org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT c.SATISFACTION, COUNT(c.ID), COALESCE(SUM(o.TOTAL_AMOUNT), 0) " +
           "FROM ORDER_FOLLOWUP_CHECKS c JOIN CUSTOMER_ORDERS o ON c.ORDER_ID = o.ID " +
           "WHERE c.SATISFACTION IS NOT NULL AND c.SATISFACTION != '' " +
           "AND (:startDate IS NULL OR (c.CREATED_AT >= :startDate AND c.CREATED_AT <= :endDate)) " +
           "GROUP BY c.SATISFACTION", nativeQuery = true)
    List<Object[]> getSatisfactionFinancialStatsByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query(value = "SELECT o.ITEMS_SUMMARY, " +
           "SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('SATISFIED', '4', '5') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('NEUTRAL', '3') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('UNSATISFIED', '1', '2') THEN 1 ELSE 0 END), " +
           "COUNT(c.ID) " +
           "FROM CUSTOMER_ORDERS o JOIN ORDER_FOLLOWUP_CHECKS c USE INDEX (idx_checks_order_sat) ON o.ID = c.ORDER_ID " +
           "WHERE (:startDate IS NULL OR (o.ORDER_DATE >= :startDate AND o.ORDER_DATE <= :endDate)) " +
           "GROUP BY o.ITEMS_SUMMARY", nativeQuery = true)
    List<Object[]> getProductSatisfactionCountsByDateRange(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('SATISFIED', '4', '5') THEN 1 ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('NEUTRAL', '3') THEN 1 ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN UPPER(c.SATISFACTION) IN ('UNSATISFIED', '1', '2') THEN 1 ELSE 0 END), 0) " +
           "FROM ORDER_FOLLOWUP_CHECKS c " +
           "WHERE (:startDate IS NULL OR (c.CREATED_AT >= :startDate AND c.CREATED_AT <= :endDate))", nativeQuery = true)
    List<Object[]> getSatisfactionCountsCombined(@Param("startDate") java.time.Instant startDate, @Param("endDate") java.time.Instant endDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE ORDER_FOLLOWUP_CHECKS ch JOIN CUSTOMER_ORDERS o ON ch.ORDER_ID = o.ID " +
                   "SET ch.STATUS = 'OVERDUE' " +
                   "WHERE ch.COMPLETED = 0 AND ch.STATUS != 'OVERDUE' " +
                   "  AND (CASE " +
                   "        WHEN ch.DURATION_UNIT = 'HOURS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE HOUR) " +
                   "        WHEN ch.DURATION_UNIT = 'DAYS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE DAY) " +
                   "        WHEN ch.DURATION_UNIT = 'WEEKS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL (ch.DURATION_VALUE * 7) DAY) " +
                   "        WHEN ch.DURATION_UNIT = 'MONTHS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE MONTH) " +
                   "        ELSE DATE_ADD(o.ORDER_DATE, INTERVAL ch.TARGET_DAYS DAY) " +
                   "      END) < UTC_TIMESTAMP()", nativeQuery = true)
    int syncOverdueChecks();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE ORDER_FOLLOWUP_CHECKS ch JOIN CUSTOMER_ORDERS o ON ch.ORDER_ID = o.ID " +
                   "SET ch.STATUS = 'PENDING' " +
                   "WHERE ch.COMPLETED = 0 AND ch.STATUS = 'OVERDUE' " +
                   "  AND (CASE " +
                   "        WHEN ch.DURATION_UNIT = 'HOURS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE HOUR) " +
                   "        WHEN ch.DURATION_UNIT = 'DAYS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE DAY) " +
                   "        WHEN ch.DURATION_UNIT = 'WEEKS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL (ch.DURATION_VALUE * 7) DAY) " +
                   "        WHEN ch.DURATION_UNIT = 'MONTHS' THEN DATE_ADD(o.ORDER_DATE, INTERVAL ch.DURATION_VALUE MONTH) " +
                   "        ELSE DATE_ADD(o.ORDER_DATE, INTERVAL ch.TARGET_DAYS DAY) " +
                   "      END) >= UTC_TIMESTAMP()", nativeQuery = true)
    int syncPendingChecks();
}
