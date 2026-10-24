package com.twekl.dashboard.service.administration;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Dashboard Service Contract (DashboardService.java)
 * ============================================================================
 * 
 * Defines all summary aggregations, KPIs, and real-time dynamic analytics charts
 * for the Executive Administration Dashboard (/admin/dashboard & /admin/stats/summary).
 * 
 * Core Capabilities:
 *   1. System Overview KPIs: Total Customers, In-Queue, Overdue, Ratings, Completion %.
 *   2. Timeline Milestones Chart: Real-time progress breakdown per active time filter preset.
 *   3. Customer Satisfaction Chart: Sentiment distribution (Satisfied, Neutral, Unsatisfied) & star ratings.
 * ============================================================================
 */
public interface DashboardService {

    /**
     * Aggregates all live dashboard statistics, KPIs, and chart series.
     * Cached with proactive eviction on any customer, check, order, feedback, or preset mutation.
     *
     * @return Complete dashboard summary payload.
     */
    Map<String, Object> getDashboardSummary();

    /**
     * Computes the completion progress, overdue, and pending rates for all currently active
     * timeline filter presets using covering MySQL index B-trees.
     *
     * @return List of active milestone breakdown maps sorted by preset display order.
     */
    List<Map<String, Object>> getMilestoneChartData();

    /**
     * Formats customer satisfaction sentiment percentages, average rating, and counts
     * across all historical orders and follow-up checkpoints.
     *
     * @param stats Base statistics map containing raw ratings.
     * @return Formatted sentiment chart series payload.
     */
    Map<String, Object> getSatisfactionChartData(Map<String, Object> stats);
}
