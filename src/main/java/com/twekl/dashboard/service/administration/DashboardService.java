package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.repository.administration.OrderFollowupCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for aggregating overview statistics and KPIs.
 * Consolidated single service file with final autowired dependencies.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AdminService adminService;
    private final RoleService roleService;
    private final CustomerService customerService;
    private final TimeFilterService timeFilterService;
    private final OrderFollowupCheckRepository followupCheckRepository;

    @Autowired
    public DashboardService(final AdminService adminService,
                            final RoleService roleService,
                            final CustomerService customerService,
                            final TimeFilterService timeFilterService,
                            final OrderFollowupCheckRepository followupCheckRepository) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.customerService = customerService;
        this.timeFilterService = timeFilterService;
        this.followupCheckRepository = followupCheckRepository;
    }

    @Cacheable("dashboardSummary")
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAdmins", adminService.count());
        summary.put("totalRoles", roleService.count());
        summary.put("totalTimePresets", timeFilterService.count());
        
        Map<String, Object> customerStats = customerService.getStats();
        summary.putAll(customerStats);

        // Dynamic chart 1: Active follow-up milestones breakdown
        List<Map<String, Object>> milestoneChart = getMilestoneChartData();
        summary.put("milestoneChart", milestoneChart);

        // Dynamic chart 2: Customer satisfaction sentiment breakdown
        Map<String, Object> satisfactionChart = getSatisfactionChartData(customerStats);
        summary.put("satisfactionChart", satisfactionChart);

        return summary;
    }

    /**
     * Highly optimized native query aggregation using covering MySQL index
     * (PRESET_ID, COMPLETED, STATUS) and active time filter presets.
     */
    public List<Map<String, Object>> getMilestoneChartData() {
        List<Object[]> rows = followupCheckRepository.getActivePresetCheckBreakdown();
        List<Map<String, Object>> list = new ArrayList<>();
        if (rows == null) return list;

        for (Object[] r : rows) {
            if (r == null || r.length < 7) continue;
            Long id = r[0] != null ? ((Number) r[0]).longValue() : null;
            String label = r[1] != null ? r[1].toString() : "Milestone " + id;
            int displayOrder = r[2] != null ? ((Number) r[2]).intValue() : 0;
            long total = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            long completed = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            long overdue = r[5] != null ? ((Number) r[5]).longValue() : 0L;
            long pending = r[6] != null ? ((Number) r[6]).longValue() : 0L;

            double completionRate = total > 0 ? Math.round(((double) completed / total) * 1000.0) / 10.0 : 0.0;
            double overdueRate = total > 0 ? Math.round(((double) overdue / total) * 1000.0) / 10.0 : 0.0;
            double pendingRate = total > 0 ? Math.max(0.0, Math.round((100.0 - completionRate - overdueRate) * 10.0) / 10.0) : 0.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("name", label);
            item.put("displayOrder", displayOrder);
            item.put("total", total);
            item.put("completed", completed);
            item.put("overdue", overdue);
            item.put("pending", pending);
            item.put("completionRate", completionRate);
            item.put("overdueRate", overdueRate);
            item.put("pendingRate", pendingRate);

            list.add(item);
        }
        return list;
    }

    /**
     * Formats customer satisfaction sentiment percentages and distribution.
     */
    public Map<String, Object> getSatisfactionChartData(Map<String, Object> stats) {
        Map<String, Object> chart = new LinkedHashMap<>();
        long satisfied = stats != null && stats.get("satisfiedCount") != null ? ((Number) stats.get("satisfiedCount")).longValue() : 0L;
        long neutral = stats != null && stats.get("neutralCount") != null ? ((Number) stats.get("neutralCount")).longValue() : 0L;
        long unsatisfied = stats != null && stats.get("unsatisfiedCount") != null ? ((Number) stats.get("unsatisfiedCount")).longValue() : 0L;
        long totalRated = satisfied + neutral + unsatisfied;

        double satisfiedPct = totalRated > 0 ? Math.round(((double) satisfied / totalRated) * 1000.0) / 10.0 : 0.0;
        double neutralPct = totalRated > 0 ? Math.round(((double) neutral / totalRated) * 1000.0) / 10.0 : 0.0;
        double unsatisfiedPct = totalRated > 0 ? Math.max(0.0, Math.round((100.0 - satisfiedPct - neutralPct) * 10.0) / 10.0) : 0.0;

        double avgRating = stats != null && stats.get("averageRating") != null ? ((Number) stats.get("averageRating")).doubleValue() : 5.0;
        double satRate = stats != null && stats.get("satisfactionRate") != null ? ((Number) stats.get("satisfactionRate")).doubleValue() : 100.0;

        chart.put("satisfiedCount", satisfied);
        chart.put("neutralCount", neutral);
        chart.put("unsatisfiedCount", unsatisfied);
        chart.put("totalRated", totalRated);
        chart.put("satisfiedPercentage", satisfiedPct);
        chart.put("neutralPercentage", neutralPct);
        chart.put("unsatisfiedPercentage", unsatisfiedPct);
        chart.put("averageRating", avgRating);
        chart.put("satisfactionRate", satRate);

        return chart;
    }
}
