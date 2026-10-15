package com.twekl.dashboard.service.administration;

import java.util.Map;

/**
 * Service contract for aggregating overview statistics and KPIs.
 * Summarizes available dashboard analytics operations.
 */
public interface DashboardService {

    Map<String, Object> getDashboardSummary();
}
