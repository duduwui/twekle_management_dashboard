package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.dto.administration.UpdateAdminRequest;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.model.administration.Role;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * Controller for Role & Permission templates management.
 * Consolidates SSR view rendering and administrative role actions.
 */
@Controller
@RequestMapping("/admin/roles")
public class RoleController {

    private final RoleService roleService;
    private final AdminService adminService;
    private final com.twekl.dashboard.security.administration.PortalAccess portalAccess;

    @Autowired
    public RoleController(final RoleService roleService,
                          final AdminService adminService,
                          final com.twekl.dashboard.security.administration.PortalAccess portalAccess) {
        this.roleService = roleService;
        this.adminService = adminService;
        this.portalAccess = portalAccess;
    }

    // =========================================================
    // SSR VIEW ENDPOINTS
    // =========================================================

    @GetMapping
    public String showRoles(@RequestParam(value = "selectedAdminId", required = false) Long selectedAdminId,
                            Model model) {
        if (!portalAccess.canViewStaff() && !portalAccess.canEditPermissions()) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("activeTab", "roles");
        List<Admin> admins = adminService.findAll();
        List<Role> roles = roleService.findAll();
        model.addAttribute("admins", admins);
        model.addAttribute("roles", roles);

        Admin selectedAdmin = null;
        if (selectedAdminId != null) {
            selectedAdmin = adminService.findById(selectedAdminId).orElse(null);
        }
        if (selectedAdmin == null && !admins.isEmpty()) {
            selectedAdmin = admins.get(0);
        }
        model.addAttribute("selectedAdmin", selectedAdmin);

        if (!model.containsAttribute("newRole")) {
            model.addAttribute("newRole", new RoleDto());
        }
        return "administration/role/roles";
    }

    @PostMapping("/create")
    public String createRoleSsr(@ModelAttribute("newRole") RoleDto roleDto, RedirectAttributes flash) {
        if (!portalAccess.canEditPermissions()) {
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to manage roles.");
            return "redirect:/admin/roles";
        }
        if (roleDto.getName() == null || roleDto.getName().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Role template name is required.");
            return "redirect:/admin/roles";
        }
        return executeRoleAction(flash, "Role template '" + roleDto.getName().trim() + "' created successfully.",
                "redirect:/admin/roles", () -> roleService.createRole(roleDto));
    }

    @PostMapping("/{roleId:\\d+}/update")
    public String updateRoleSsr(@PathVariable("roleId") Long roleId,
                                @ModelAttribute RoleDto roleDto,
                                RedirectAttributes flash) {
        if (!portalAccess.canEditPermissions()) {
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to manage roles.");
            return "redirect:/admin/roles";
        }
        return executeRoleAction(flash, "Role template updated successfully.",
                "redirect:/admin/roles", () -> roleService.updateRole(roleId, roleDto));
    }

    @PostMapping({"/assign_user_role", "/{adminId:\\d+}/assign_user_role"})
    public String assignAdminRoleSsr(@PathVariable(value = "adminId", required = false) Long pathAdminId,
                                     @RequestParam(value = "adminId", required = false) Long paramAdminId,
                                     @RequestParam(value = "roleIds", required = false) Set<Long> roleIds,
                                     @RequestParam(value = "roleId", required = false) Long roleId,
                                     @ModelAttribute UpdateAdminRequest updateAdminRequest,
                                     RedirectAttributes flash) {
        if (!portalAccess.canEditPermissions()) {
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to modify staff permissions.");
            return "redirect:/admin/roles";
        }
        Long targetAdminId = pathAdminId != null ? pathAdminId : paramAdminId;
        if (targetAdminId == null) {
            flash.addFlashAttribute("errorMessage", "No administrator was selected for role assignment.");
            return "redirect:/admin/roles";
        }

        return executeRoleAction(flash, "Assigned permissions updated successfully.",
                "redirect:/admin/roles?selectedAdminId=" + targetAdminId, () -> {
                    Set<Long> targetRoleIds = new HashSet<>();
                    if (roleIds != null && !roleIds.isEmpty()) {
                        targetRoleIds.addAll(roleIds);
                    } else if (roleId != null && roleId > 0) {
                        targetRoleIds.add(roleId);
                    } else if (updateAdminRequest.getRoleIds() != null && !updateAdminRequest.getRoleIds().isEmpty()) {
                        targetRoleIds.addAll(updateAdminRequest.getRoleIds());
                    }

                    UpdateAdminRequest updateReq = new UpdateAdminRequest();
                    updateReq.setRoleIds(targetRoleIds);
                    if (updateAdminRequest.getPermissions() != null) {
                        updateReq.setPermissions(updateAdminRequest.getPermissions());
                    }
                    adminService.updateAdmin(targetAdminId, updateReq);
                });
    }

    @PostMapping("/{roleId:\\d+}/delete")
    public String deleteRoleSsr(@PathVariable("roleId") Long roleId, RedirectAttributes flash) {
        if (!portalAccess.canEditPermissions()) {
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to delete roles.");
            return "redirect:/admin/roles";
        }
        return executeRoleAction(flash, "Role template deleted successfully.",
                "redirect:/admin/roles", () -> roleService.deleteRole(roleId));
    }

    // =========================================================
    // REUSABLE HELPER ENGINE
    // =========================================================

    @FunctionalInterface
    private interface ActionCallback {
        void execute() throws Exception;
    }

    /**
     * Reusable logic block that manages try-catch boundaries and routing dynamically.
     * Consolidates error feedback pipelines into one shared engine.
     */
    private String executeRoleAction(RedirectAttributes flash, String successMessage, String redirectTarget, ActionCallback action) {
        try {
            action.execute();
            flash.addFlashAttribute("successMessage", successMessage);
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirectTarget;
    }
}
