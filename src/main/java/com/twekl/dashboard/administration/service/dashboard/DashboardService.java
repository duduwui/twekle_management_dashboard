package com.twekl.dashboard.administration.service.dashboard;

import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.customer.CustomerService;
import com.twekl.dashboard.administration.service.role.RoleService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterPresetService;
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

    private final AdminService adminService;
    private final RoleService roleService;
    private final CustomerService customerService;
    private final TimeFilterPresetService timeFilterPresetService;

    public DashboardService(AdminService adminService,
                            RoleService roleService,
                            CustomerService customerService,
                            TimeFilterPresetService timeFilterPresetService) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.customerService = customerService;
        this.timeFilterPresetService = timeFilterPresetService;
    }

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAdmins", adminService.count());
        summary.put("totalRoles", roleService.count());
        summary.put("totalTimePresets", timeFilterPresetService.count());
        summary.putAll(customerService.getStats());
        return summary;
    }
}
