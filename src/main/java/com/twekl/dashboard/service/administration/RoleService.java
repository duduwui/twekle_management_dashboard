package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.model.administration.Role;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * Security Role Service Contract (RoleService.java)
 * ============================================================================
 * 
 * Defines all role management operations and security capability templates
 * applied across administrative staff members.
 * 
 * Core Capabilities:
 *   1. Role Catalog: Lists and queries available administrative security roles.
 *   2. Custom Roles: Creates and edits custom roles with designated CRUD and action permissions.
 *   3. System Safeguards: Protects foundational roles (such as 'Software Engineer') from accidental deletion.
 *   4. Referential Integrity: Automatically severs role memberships from active staff on role deletion.
 * ============================================================================
 */
public interface RoleService {

    /**
     * Retrieves all defined administrative roles.
     *
     * @return List of Role entities.
     */
    List<Role> findAll();

    /**
     * Looks up a security role by its primary key ID.
     *
     * @param roleId Role primary key ID.
     * @return Optional containing Role if found.
     */
    Optional<Role> findById(Long roleId);

    /**
     * Returns total count of registered roles.
     *
     * @return Total count.
     */
    long count();

    /**
     * Creates a new administrative role with unique name and specified CRUD permissions.
     *
     * @param req RoleDto payload containing name and permission flags.
     * @return Persisted Role entity.
     */
    Role createRole(RoleDto req);

    /**
     * Updates an existing role's name, CRUD permissions, and action capabilities.
     *
     * @param roleId ID of the role to update.
     * @param req    RoleDto payload with updated attributes.
     * @return Updated Role entity.
     */
    Role updateRole(Long roleId, RoleDto req);

    /**
     * Deletes a role from the catalog and disassociates it from all assigned administrators.
     * System base roles cannot be deleted.
     *
     * @param roleId ID of the role to delete.
     */
    void deleteRole(Long roleId);
}
