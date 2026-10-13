package com.twekl.dashboard.service.administration;

import java.util.Map;

/**
 * Service contract for aggregating Dashboard Overview Statistics and KPIs.
 */
public interface DashboardService {

    Map<String, Object> getDashboardSummary();
}
