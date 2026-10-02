package com.twekl.dashboard.modules.stats;

import com.twekl.dashboard.modules.admin.AdminRepository;
import com.twekl.dashboard.modules.role.RoleRepository;
import com.twekl.dashboard.modules.user.AppUserRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/internal/stats", "/api/stats"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class DashboardStatsController {

    private final AdminRepository adminRepository;
    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public DashboardStatsController(AdminRepository adminRepository, AppUserRepository userRepository, RoleRepository roleRepository) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

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
