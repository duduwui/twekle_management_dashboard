package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.dto.administration.UpdateAdminRequest;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

/**
 * Controller interface contract for Role & Permissions Operations.
 * Provides a clean high-level summary of role assignment and management actions.
 */
public interface RoleOperations {

    String showRoles(Long selectedAdminId, Model model);

    String createRoleSsr(RoleDto request, RedirectAttributes flash);

    String updateRoleSsr(Long roleId, RoleDto request, RedirectAttributes flash);

    String assignAdminRoleSsr(Long pathAdminId, Long paramAdminId, Set<Long> roleIds, Long roleId, UpdateAdminRequest request, RedirectAttributes flash);

    String deleteRoleSsr(Long roleId, RedirectAttributes flash);
}
