package com.twekl.dashboard.modules.stats.controller;

import com.twekl.dashboard.modules.admin.service.AdminService;
import com.twekl.dashboard.modules.customer.service.CustomerService;
import com.twekl.dashboard.modules.role.service.RoleService;
import com.twekl.dashboard.modules.timefilter.service.TimeFilterPresetService;
import com.twekl.dashboard.modules.user.service.AppUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/internal/stats", "/api/stats"})
public class DashboardStatsController {

    private final AdminService adminService;
    private final AppUserService userService;
    private final RoleService roleService;
    private final CustomerService customerService;
    private final TimeFilterPresetService timeFilterPresetService;

    public DashboardStatsController(AdminService adminService,
                                    AppUserService userService,
                                    RoleService roleService,
                                    CustomerService customerService,
                                    TimeFilterPresetService timeFilterPresetService) {
        this.adminService = adminService;
        this.userService = userService;
        this.roleService = roleService;
        this.customerService = customerService;
        this.timeFilterPresetService = timeFilterPresetService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAdmins", adminService.count());
        summary.put("totalUsers", userService.count());
        summary.put("totalRoles", roleService.count());
        summary.put("totalTimePresets", timeFilterPresetService.count());
        summary.putAll(customerService.getStats());
        return ResponseEntity.ok(summary);
    }
}
