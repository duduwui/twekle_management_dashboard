package com.twekl.dashboard.security.administration;

import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.model.administration.Role;
import com.twekl.dashboard.service.administration.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Enterprise Access Control and Permission Evaluator.
 * Centralized inside the security folder for full visibility over:
 * - Current authenticated administrator identity
 * - Live role and permission evaluations
 * - Password hashing and validation utilities
 * Loads live permissions once per HTTP request so privilege changes apply immediately.
 */
@Component("portalAccess")
@RequestScope
public class PortalAccess {

    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;
    private Admin current;
    private boolean loaded;

    @Autowired
    public PortalAccess(final AdminService adminService,
                        final PasswordEncoder passwordEncoder) {
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================
    // CURRENT AUTHENTICATED USER
    // =========================================================

    public Admin getCurrent() {
        if (!loaded) {
            current = adminService.getCurrentAdmin().filter(Admin::isActive).orElse(null);
            loaded = true;
        }
        return current;
    }

    // =========================================================
    // PASSWORD ENCRYPTION & VERIFICATION (BCRYPT)
    // =========================================================

    /**
     * Encrypts a raw plain-text password using BCrypt hashing.
     */
    public String encryptPassword(final String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        return passwordEncoder.encode(rawPassword.trim());
    }

    /**
     * Verifies a raw plain-text password against a stored BCrypt hash.
     */
    public boolean verifyPassword(final String rawPassword, final String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // =========================================================
    // ROLE & PERMISSION CHECKS
    // =========================================================

    public boolean isSuperAdmin() {
        Admin admin = getCurrent();
        return admin != null && admin.isSuperAdmin();
    }

    public boolean hasRole(final String roleName) {
        Admin admin = getCurrent();
        if (admin == null || roleName == null) return false;
        if (admin.isSuperAdmin()) return true;
        if (admin.getRoles() == null) return false;
        for (Role r : admin.getRoles()) {
            if (r != null && roleName.equalsIgnoreCase(r.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPermission(final String moduleKey, final String action) {
        return adminService.hasPermission(getCurrent(), moduleKey, action);
    }

    public boolean canManageUsers() {
        Admin admin = getCurrent();
        return admin != null && (admin.isSuperAdmin() || admin.isCanManageUsers() || hasPermission("ADMIN_MANAGEMENT", "READ"));
    }

    public boolean canCreateRoles() {
        Admin admin = getCurrent();
        return admin != null && (admin.isSuperAdmin() || admin.isCanCreateRoles() || hasPermission("ROLE_MANAGEMENT", "READ"));
    }

    public boolean canUpdateFollowups() {
        Admin admin = getCurrent();
        return admin != null && (admin.isSuperAdmin() || admin.isCanUpdateFollowups() || hasPermission("CUSTOMER_FOLLOWUP", "UPDATE"));
    }

    public boolean canReadReports() {
        Admin admin = getCurrent();
        return admin != null && (admin.isSuperAdmin() || admin.isCanReadReports() || hasPermission("REPORTS_INSIGHTS", "READ"));
    }

    // Template convenience shortcuts
    public boolean staff() {
        return canManageUsers();
    }

    public boolean roles() {
        return canCreateRoles();
    }
}
