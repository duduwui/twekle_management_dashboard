package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.dto.administration.UpdateAdminRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

/**
 * ============================================================================
 * Role Controller Operations Contract (RoleOperations.java)
 * ============================================================================
 * 
 * Defines all Web SSR endpoints for managing administrative security roles,
 * permission templates, and staff administrator assignment.
 * 
 * Canonical Route: /admin/roles
 * 
 * Core Capabilities:
 *   1. Role Catalog View: SSR display of role templates with permissions matrix.
 *   2. Template Actions: Creation, modification, and safe deletion of custom roles.
 *   3. Staff Assignment: Associates roles and granular permissions with staff accounts.
 * ============================================================================
 */
public interface RoleOperations {

    /**
     * Renders the administrative roles catalog and staff assignment page.
     *
     * @param selectedAdminId Optional ID of the currently focused administrator.
     * @param model           Spring MVC Model container.
     * @return Thymeleaf view path: "administration/role/roles".
     */
    String showRoles(Long selectedAdminId, Model model);

    /**
     * Creates a new administrative security role template.
     *
     * @param roleDto RoleDto payload containing name and CRUD permissions.
     * @param flash   RedirectAttributes container for flash notifications.
     * @return Redirect to "/admin/roles".
     */
    String createRoleSsr(RoleDto roleDto, RedirectAttributes flash);

    /**
     * Updates an existing security role's CRUD flags and custom actions.
     *
     * @param roleId  Role primary key ID.
     * @param roleDto Updated attributes.
     * @param flash   RedirectAttributes container.
     * @return Redirect to "/admin/roles".
     */
    String updateRoleSsr(Long roleId, RoleDto roleDto, RedirectAttributes flash);

    /**
     * Assigns roles and module permissions to a target staff administrator account.
     *
     * @param pathAdminId        Admin ID from URI path.
     * @param paramAdminId       Admin ID from request parameter.
     * @param roleIds            Set of assigned role IDs.
     * @param roleId             Single assigned role ID.
     * @param updateAdminRequest UpdateAdminRequest payload.
     * @param request            HttpServletRequest container for inspecting AJAX headers.
     * @param flash              RedirectAttributes container.
     * @return Redirect to "/admin/roles?selectedAdminId=..." or JSON response for AJAX.
     */
    Object assignAdminRoleSsr(
            Long pathAdminId,
            Long paramAdminId,
            Set<Long> roleIds,
            Long roleId,
            UpdateAdminRequest updateAdminRequest,
            HttpServletRequest request,
            RedirectAttributes flash);

    /**
     * Deletes a custom security role template. Base roles are protected from deletion.
     *
     * @param roleId  ID of the role to delete.
     * @param request HttpServletRequest container for inspecting AJAX headers.
     * @param flash   RedirectAttributes container.
     * @return Redirect to "/admin/roles" or JSON response for AJAX.
     */
    Object deleteRoleSsr(Long roleId, HttpServletRequest request, RedirectAttributes flash);
}
