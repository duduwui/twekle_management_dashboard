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
 * Functional interface mapping transactional actions that can throw exceptions.
 * Enables clean 1-line execution blocks inside the controller endpoints.
 */
@FunctionalInterface
interface RoleAction {
    void execute() throws Exception;
}

/**
 * Interface contract for Role and Permission Operations.
 * Defined at the top of the file for full visibility.
 */
interface RoleOperations {
    String showRoles(Long selectedAdminId, Model model);
    String createRoleSsr(RoleDto request, RedirectAttributes flash);
    String updateRoleSsr(Long roleId, RoleDto request, RedirectAttributes flash);
    String assignAdminRoleSsr(Long pathAdminId, Long paramAdminId, Set<Long> roleIds, Long roleId, UpdateAdminRequest request, RedirectAttributes flash);
    String deleteRoleSsr(Long roleId, RedirectAttributes flash);
}

/**
 * Controller for Role & Permission templates management.
 * Consolidates SSR view rendering and administrative role actions.
 * Implements RoleOperations interface contract.
 */
@Controller
@RequestMapping("/admin/roles")
public class RoleController implements RoleOperations {

    private final RoleService roleService;
    private final AdminService adminService;

    @Autowired
    public RoleController(final RoleService roleService, final AdminService adminService) {
        this.roleService = roleService;
        this.adminService = adminService;
    }

    // =========================================================
    // SSR VIEW ENDPOINTS (RoleOperations Contract)
    // =========================================================

    @Override
    @GetMapping
    public String showRoles(@RequestParam(value = "selectedAdminId", required = false) Long selectedAdminId,
                            Model model) {
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

    @Override
    @PostMapping("/create")
    public String createRoleSsr(@ModelAttribute("newRole") RoleDto request, RedirectAttributes flash) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Role template name is required.");
            return "redirect:/admin/roles";
        }
        return executeRoleAction(flash, "Role template '" + request.getName().trim() + "' created successfully.",
                "redirect:/admin/roles", () -> roleService.createRole(request));
    }

    @Override
    @PostMapping("/{roleId:\\d+}/update")
    public String updateRoleSsr(@PathVariable("roleId") Long roleId,
                                @ModelAttribute RoleDto request,
                                RedirectAttributes flash) {
        return executeRoleAction(flash, "Role template updated successfully.",
                "redirect:/admin/roles", () -> roleService.updateRole(roleId, request));
    }

    @Override
    @PostMapping("/assign_user_role")
    public String assignAdminRoleSsr(@PathVariable(value = "adminId", required = false) Long pathAdminId,
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

        return executeRoleAction(flash, "Assigned permissions updated successfully.",
                "redirect:/admin/roles?selectedAdminId=" + targetAdminId, () -> {
                    Set<Long> targetRoleIds = new HashSet<>();
                    if (roleIds != null && !roleIds.isEmpty()) {
                        targetRoleIds.addAll(roleIds);
                    } else if (roleId != null && roleId > 0) {
                        targetRoleIds.add(roleId);
                    } else if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
                        targetRoleIds.addAll(request.getRoleIds());
                    }

                    UpdateAdminRequest updateReq = new UpdateAdminRequest();
                    updateReq.setRoleIds(targetRoleIds);
                    if (request.getPermissions() != null) {
                        updateReq.setPermissions(request.getPermissions());
                    }
                    adminService.updateAdmin(targetAdminId, updateReq);
                });
    }

    @Override
    @PostMapping("/{roleId:\\d+}/delete")
    public String deleteRoleSsr(@PathVariable("roleId") Long roleId, RedirectAttributes flash) {
        return executeRoleAction(flash, "Role template deleted successfully.",
                "redirect:/admin/roles", () -> roleService.deleteRole(roleId));
    }

    // =========================================================
    // REUSABLE HELPER ENGINE
    // =========================================================

    /**
     * Reusable logic block that manages try-catch boundaries and routing dynamically.
     * Consolidates error feedback pipelines into one shared engine.
     */
    private String executeRoleAction(RedirectAttributes flash, String successMessage, String redirectTarget, RoleAction action) {
        try {
            action.execute();
            flash.addFlashAttribute("successMessage", successMessage);
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirectTarget;
    }
}
