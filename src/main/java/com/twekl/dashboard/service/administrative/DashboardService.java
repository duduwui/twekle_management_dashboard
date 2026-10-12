package com.twekl.dashboard.service.administrative;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for aggregating Dashboard Overview Statistics and KPIs.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AdminService adminService;
    private final RoleService roleService;
    private final CustomerService customerService;
    private final TimeFilterService timeFilterService;

    @Autowired
    public DashboardService(AdminService adminService,
                            RoleService roleService,
                            CustomerService customerService,
                            TimeFilterService timeFilterService) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.customerService = customerService;
        this.timeFilterService = timeFilterService;
    }

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAdmins", adminService.count());
        summary.put("totalRoles", roleService.count());
        summary.put("totalTimePresets", timeFilterService.count());
        summary.putAll(customerService.getStats());
        return summary;
    }
}
