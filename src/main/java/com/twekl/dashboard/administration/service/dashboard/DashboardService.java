package com.twekl.dashboard.administration.service.dashboard;

import com.twekl.dashboard.administration.service.admin.AdminQueryService;
import com.twekl.dashboard.administration.service.customer.CustomerStatsService;
import com.twekl.dashboard.administration.service.role.RoleQueryService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for aggregating Dashboard Overview Statistics and KPIs.
 * Dedicated dashboard domain service.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AdminQueryService adminQueryService;
    private final RoleQueryService roleQueryService;
    private final CustomerStatsService customerStatsService;
    private final TimeFilterQueryService timeFilterQueryService;

    public DashboardService(AdminQueryService adminQueryService,
                            RoleQueryService roleQueryService,
                            CustomerStatsService customerStatsService,
                            TimeFilterQueryService timeFilterQueryService) {
        this.adminQueryService = adminQueryService;
        this.roleQueryService = roleQueryService;
        this.customerStatsService = customerStatsService;
        this.timeFilterQueryService = timeFilterQueryService;
    }

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAdmins", adminQueryService.count());
        summary.put("totalRoles", roleQueryService.count());
        summary.put("totalTimePresets", timeFilterQueryService.count());
        summary.putAll(customerStatsService.getStats());
        return summary;
    }
}
