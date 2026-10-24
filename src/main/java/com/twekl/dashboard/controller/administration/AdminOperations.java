package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ============================================================================
 * Staff Administrator Controller Operations Contract (AdminOperations.java)
 * ============================================================================
 * 
 * Defines all Web SSR endpoints for managing administrator staff accounts,
 * status toggling, and staff account creation.
 * 
 * Canonical Route: /admin/admins
 * 
 * Core Capabilities:
 *   1. Staff Directory View: SSR rendering of administrator accounts with roles and permissions.
 *   2. Account Registration: Validated creation of new staff users with encrypted credentials.
 *   3. Status Switch: Toggles active status with support for both SSR redirects and live AJAX requests.
 *   4. Account Deletion: Safely removes staff accounts with protection for primary super administrator.
 * ============================================================================
 */
public interface AdminOperations {

    /**
     * Renders the staff administrators directory SSR HTML page.
     *
     * @param model Spring MVC Model container.
     * @return Thymeleaf view path: "administration/admin/admins".
     */
    String showAdmins(Model model);

    /**
     * Creates a new staff administrator account.
     *
     * @param request       Validated CreateAdminRequest payload.
     * @param bindingResult Validation result holder.
     * @param flash         RedirectAttributes for flash feedback.
     * @return Redirect to "/admin/admins".
     */
    String createAdminSsr(CreateAdminRequest request, BindingResult bindingResult, RedirectAttributes flash);

    /**
     * Toggles an administrator's active status (active <-> inactive).
     * Supports both HTML redirect and AJAX JSON responses.
     *
     * @param adminId     Target administrator ID.
     * @param redirectUrl Optional redirect URL.
     * @param request     HttpServletRequest to inspect headers (isAjax).
     * @param flash       RedirectAttributes container.
     * @return Redirect string or ResponseEntity depending on request format.
     */
    Object toggleAdminStatusSsr(Long adminId, String redirectUrl, HttpServletRequest request, RedirectAttributes flash);

    /**
     * Permanently deletes a staff administrator account.
     *
     * @param adminId Target administrator ID.
     * @param flash   RedirectAttributes container.
     * @return Redirect to "/admin/admins".
     */
    String deleteAdminSsr(Long adminId, RedirectAttributes flash);
}
