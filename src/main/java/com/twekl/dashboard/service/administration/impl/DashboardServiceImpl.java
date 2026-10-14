package com.twekl.dashboard.service.administration.impl;

import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.CustomerService;
import com.twekl.dashboard.service.administration.DashboardService;
import com.twekl.dashboard.service.administration.RoleService;
import com.twekl.dashboard.service.administration.TimeFilterService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of DashboardService for aggregating overview statistics and KPIs.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final AdminService adminService;
    private final RoleService roleService;
    private final CustomerService customerService;
    private final TimeFilterService timeFilterService;

    @Autowired
    public DashboardServiceImpl(AdminService adminService,
                               RoleService roleService,
                               CustomerService customerService,
                               TimeFilterService timeFilterService) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.customerService = customerService;
        this.timeFilterService = timeFilterService;
    }

    @Override
    @Cacheable("dashboardSummary")
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAdmins", adminService.count());
        summary.put("totalRoles", roleService.count());
        summary.put("totalTimePresets", timeFilterService.count());
        summary.putAll(customerService.getStats());
        return summary;
    }
}
