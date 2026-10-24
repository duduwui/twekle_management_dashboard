package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.dto.administration.UpdateAdminRequest;
import com.twekl.dashboard.model.administration.Admin;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * Staff & Administrator Service Contract (AdminService.java)
 * ============================================================================
 * 
 * Defines all administrator account management, role assignment, and granular
 * action-level permission enforcement (CREATE, READ, UPDATE, DELETE).
 * 
 * Core Capabilities:
 *   1. Account Lifecycle: Creation, update, deactivation, and safe deletion of staff accounts.
 *   2. Authentication & Context: Retrieves current authenticated administrator session.
 *   3. Granular Authorization: Checks module-level CRUD permissions and action flags.
 *   4. Super Admin Safeguards: Protects the primary 'twekl_super_admin' account from deactivation or deletion.
 * ============================================================================
 */
public interface AdminService {

    /**
     * Retrieves all administrator accounts.
     *
     * @return List of all Admin entities.
     */
    List<Admin> findAll();

    /**
     * Looks up an administrator by their primary key ID.
     *
     * @param adminId Admin primary key ID.
     * @return Optional containing Admin if found.
     */
    Optional<Admin> findById(Long adminId);

    /**
     * Looks up an administrator by their unique username.
     *
     * @param username Account username.
     * @return Optional containing Admin if found.
     */
    Optional<Admin> findByUsername(String username);

    /**
     * Returns total count of registered administrator accounts.
     *
     * @return Total count.
     */
    long count();

    /**
     * Resolves the currently authenticated administrator from Spring Security's SecurityContextHolder.
     *
     * @return Optional containing current Admin entity, or empty if anonymous.
     */
    Optional<Admin> getCurrentAdmin();

    /**
     * Evaluates whether an administrator is authorized to execute a specific action on a module.
     * Super administrators are unconditionally granted access.
     *
     * @param admin     Admin entity to evaluate.
     * @param moduleKey Module identifier (e.g., 'CUSTOMER_FOLLOWUP', 'REPORTS_INSIGHTS').
     * @param action    Action verb ('CREATE', 'READ', 'UPDATE', 'DELETE', or custom action).
     * @return true if authorized; false otherwise.
     */
    boolean hasPermission(Admin admin, String moduleKey, String action);

    /**
     * Creates a new administrator account with BCrypt encrypted password and role assignments.
     *
     * @param req CreateAdminRequest payload.
     * @return Persisted Admin entity.
     */
    Admin createAdmin(CreateAdminRequest req);

    /**
     * Updates an administrator's profile, roles, password, and granular permissions.
     *
     * @param adminId ID of the admin to update.
     * @param req     UpdateAdminRequest payload.
     * @return Updated Admin entity.
     */
    Admin updateAdmin(Long adminId, UpdateAdminRequest req);

    /**
     * Toggles an admin's active status (active <-> inactive). Primary super admin cannot be deactivated.
     *
     * @param adminId ID of the admin account.
     * @return Updated Admin entity.
     */
    Admin toggleStatus(Long adminId);

    /**
     * Permanently deletes an administrator account. Primary super admin cannot be deleted.
     *
     * @param adminId ID of the admin account to delete.
     */
    void deleteAdmin(Long adminId);

    /**
     * Populates default baseline permissions for newly registered administrator accounts.
     *
     * @param admin Admin entity to seed permissions for.
     */
    void seedDefaultPermissions(Admin admin);
}
