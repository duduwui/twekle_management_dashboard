package com.twekl.dashboard.administration.controller.role;

import com.twekl.dashboard.administration.dto.admin.UpdateAdminRequest;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.service.admin.AdminUpdateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.Set;

/**
 * Assigning Roles to Administrators.
 */
@Controller
@RequestMapping("/admin/roles")
public class RoleAssign {

    private final AdminUpdateService adminUpdateService;

    public RoleAssign(AdminUpdateService adminUpdateService) {
        this.adminUpdateService = adminUpdateService;
    }

    @PostMapping({"/{adminId:\\d+}/assign", "/assign"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS', 'PERMISSION_CREATE_ROLES')")
    public String assignAdminRole(@PathVariable(value = "adminId", required = false) Long pathAdminId,
                                  @RequestParam(value = "adminId", required = false) Long paramAdminId,
                                  @RequestParam(value = "roleIds", required = false) Set<Long> roleIds,
                                  @RequestParam(value = "roleId", required = false) Long roleId,
                                  @ModelAttribute UpdateAdminRequest request,
                                  RedirectAttributes flash) {
        Long targetAdminId = pathAdminId != null ? pathAdminId : paramAdminId;
        if (targetAdminId == null) {
            flash.addFlashAttribute("errorMessage", "No administrator was selected for role assignment.");
            return "redirect:/admin/roles";
        }

        try {
            if (roleIds != null && !roleIds.isEmpty()) {
                request.setRoleIds(roleIds);
            } else if (roleId != null) {
                Set<Long> set = new HashSet<>();
                set.add(roleId);
                request.setRoleIds(set);
            }

            Admin updated = adminUpdateService.updateAdmin(targetAdminId, request);
            flash.addFlashAttribute("successMessage", "Role and permissions successfully assigned to administrator '" + updated.getUsername() + "'.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles?selectedAdminId=" + targetAdminId;
    }
}
