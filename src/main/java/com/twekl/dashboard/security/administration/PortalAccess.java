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

    public boolean hasAction(final String actionKey) {
        Admin admin = getCurrent();
        return admin != null && admin.hasAction(actionKey);
    }

    // Customer Follow-ups Actions
    public boolean canViewCustomerList() { return hasAction("CUSTOMER_VIEW_LIST"); }
    public boolean canViewCustomerLog() { return hasAction("CUSTOMER_VIEW_LOG"); }
    public boolean canToggleCheck() { return hasAction("CUSTOMER_CHECK_TOGGLE"); }
    public boolean canAddFeedback() { return hasAction("CUSTOMER_ADD_FEEDBACK"); }
    public boolean canUploadPhotos() { return hasAction("CUSTOMER_UPLOAD_PHOTOS"); }
    public boolean canExportCustomers() { return hasAction("CUSTOMER_EXPORT_EXCEL"); }

    // Reports & Analytics Actions
    public boolean canViewReportMilestones() { return hasAction("REPORT_VIEW_MILESTONES"); }
    public boolean canViewReportSatisfaction() { return hasAction("REPORT_VIEW_SATISFACTION"); }
    public boolean canViewReportProducts() { return hasAction("REPORT_VIEW_PRODUCTS"); }
    public boolean canViewReportRetention() { return hasAction("REPORT_VIEW_RETENTION"); }
    public boolean canExportReports() { return hasAction("REPORT_EXPORT_EXCEL"); }

    // Staff & Roles Actions
    public boolean canViewStaff() { return hasAction("STAFF_VIEW"); }
    public boolean canManageStaff() { return hasAction("STAFF_MANAGE"); }
    public boolean canEditPermissions() { return hasAction("STAFF_PERMISSIONS"); }

    // Time Filter Presets Actions
    public boolean canViewPresets() { return hasAction("PRESETS_VIEW"); }
    public boolean canManagePresets() { return hasAction("PRESETS_MANAGE"); }

    public boolean canManageUsers() {
        Admin admin = getCurrent();
        return admin != null && (admin.isSuperAdmin() || hasAction("STAFF_MANAGE") || admin.isCanManageUsers());
    }

    public boolean canCreateRoles() {
        Admin admin = getCurrent();
        return admin != null && (admin.isSuperAdmin() || hasAction("STAFF_PERMISSIONS") || admin.isCanCreateRoles());
    }

    public boolean canUpdateFollowups() {
        Admin admin = getCurrent();
        return admin != null && (admin.isSuperAdmin() || hasAction("CUSTOMER_CHECK_TOGGLE") || admin.isCanUpdateFollowups());
    }

    public boolean canReadReports() {
        Admin admin = getCurrent();
        return admin != null && (admin.isSuperAdmin() || admin.isCanReadReports());
    }

    // Template convenience shortcuts
    public boolean staff() {
        return canViewStaff() || canManageStaff();
    }

    public boolean roles() {
        return canEditPermissions();
    }
}
