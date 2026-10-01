package com.twekl.dashboard.controller.api.analytics;

import com.twekl.dashboard.repository.AdminRepository;
import com.twekl.dashboard.repository.AppUserRepository;
import com.twekl.dashboard.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Analytics", description = "System summary statistics, account totals, and KPI dashboard metrics")
@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class DashboardStatsApiController {

    private final AdminRepository adminRepository;
    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public DashboardStatsApiController(AdminRepository adminRepository, AppUserRepository userRepository, RoleRepository roleRepository) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Operation(summary = "Get system summary statistics", description = "Returns active/total counts for administrators, staff users, and configured role templates")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryStats() {
        long totalAdmins = adminRepository.count();
        long activeAdmins = adminRepository.findAll().stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .count();

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .count();

        long totalRoles = roleRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAdmins", totalAdmins);
        stats.put("activeAdmins", activeAdmins);
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalRoles", totalRoles);

        return ResponseEntity.ok(stats);
    }
}
