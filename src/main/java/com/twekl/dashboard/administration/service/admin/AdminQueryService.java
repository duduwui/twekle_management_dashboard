package com.twekl.dashboard.administration.service.admin;

import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.model.admin.AdminPermission;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.repository.admin.AdminRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AdminQueryService {

    private final AdminRepository adminRepository;

    public AdminQueryService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    public Optional<Admin> findById(Long adminId) {
        return adminRepository.findById(adminId);
    }

    public Optional<Admin> findByAdminId(Long adminId) {
        return adminRepository.findById(adminId);
    }

    public Optional<Admin> findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    public long count() {
        return adminRepository.count();
    }

    public Optional<Admin> getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return findByUsername(auth.getName());
    }

    public boolean hasPermission(Admin admin, String moduleKey, String action) {
        if (admin == null || moduleKey == null || action == null) return false;
        if (admin.isSuperAdmin()) return true;
        if (admin.getPermissions() != null && !admin.getPermissions().isEmpty()) {
            for (AdminPermission p : admin.getPermissions()) {
                if (p.getModuleKey().equalsIgnoreCase(moduleKey)) {
                    return switch (action.toUpperCase()) {
                        case "CREATE" -> p.isCanCreate();
                        case "READ" -> p.isCanRead();
                        case "UPDATE" -> p.isCanUpdate();
                        case "DELETE" -> p.isCanDelete();
                        default -> false;
                    };
                }
            }
            return false;
        }
        if (admin.getRoles() != null && !admin.getRoles().isEmpty()) {
            return switch (action.toUpperCase()) {
                case "CREATE" -> admin.getRoles().stream().anyMatch(Role::isCanCreate);
                case "READ" -> admin.getRoles().stream().anyMatch(Role::isCanRead);
                case "UPDATE" -> admin.getRoles().stream().anyMatch(Role::isCanUpdate);
                case "DELETE" -> admin.getRoles().stream().anyMatch(Role::isCanDelete);
                default -> false;
            };
        }
        return false;
    }

    public boolean hasCurrentUserPermission(String moduleKey, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        boolean hasSuperRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (hasSuperRole) {
            return true;
        }
        return findByUsername(auth.getName())
                .map(admin -> hasPermission(admin, moduleKey, action))
                .orElse(false);
    }
}
