package com.twekl.dashboard.administration.controller.report;

import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.customer.CustomerService;
import com.twekl.dashboard.administration.service.role.RoleService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterPresetService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for Summary Statistics and KPIs.
 * Standardized under /admin/stats route.
 */
@RestController
@RequestMapping({"/admin/stats", "/admin/api/stats", "/internal/stats", "/api/stats"})
public class DashboardStatsController {

    private final AdminService adminService;
    private final RoleService roleService;
    private final CustomerService customerService;
    private final TimeFilterPresetService timeFilterPresetService;

    public DashboardStatsController(AdminService adminService,
                                    RoleService roleService,
                                    CustomerService customerService,
                                    TimeFilterPresetService timeFilterPresetService) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.customerService = customerService;
        this.timeFilterPresetService = timeFilterPresetService;
    }

    @GetMapping(value = {"", "/summary"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAdmins", adminService.count());
        summary.put("totalRoles", roleService.count());
        summary.put("totalTimePresets", timeFilterPresetService.count());
        summary.putAll(customerService.getStats());
        return ResponseEntity.ok(summary);
    }
}
