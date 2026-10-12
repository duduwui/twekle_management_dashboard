package com.twekl.dashboard.administration.controller.role;

import com.twekl.dashboard.administration.dto.admin.UpdateAdminRequest;
import com.twekl.dashboard.administration.dto.role.RoleDto;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.role.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for Managing Role Templates & Associated Permissions.
 * Pure Thymeleaf Server-Side Rendering (SSR) with Post-Redirect-Get (PRG) pattern.
 */
@Controller
@RequestMapping("/admin/roles")
public class RoleController {

    private final RoleService roleService;
    private final AdminService adminService;

    public RoleController(RoleService roleService, AdminService adminService) {
        this.roleService = roleService;
        this.adminService = adminService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    @GetMapping
    public String showRoles(@RequestParam(value = "selectedAdminId", required = false) Long selectedAdminId,
                            Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "roles");
        List<Admin> adminList = adminService.findAll();
        List<Role> roleList = roleService.findAll();
        model.addAttribute("adminList", adminList);
        model.addAttribute("roleList", roleList);
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));

        Admin selectedAdmin = null;
        if (selectedAdminId != null) {
            selectedAdmin = adminList.stream().filter(a -> a.getAdminId().equals(selectedAdminId)).findFirst().orElse(null);
        }
        if (selectedAdmin == null && !adminList.isEmpty()) {
            selectedAdmin = adminList.get(0);
        }
        model.addAttribute("selectedAdmin", selectedAdmin);

        if (!model.containsAttribute("newRole")) {
            model.addAttribute("newRole", new RoleDto());
        }
        return "administration/role/roles";
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

            Admin updated = adminService.updateAdmin(targetAdminId, request);
            flash.addFlashAttribute("successMessage", "Role and permissions successfully assigned to administrator '" + updated.getUsername() + "'.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles?selectedAdminId=" + targetAdminId;
    }

    @PostMapping({"/create", "/create-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String createRole(@ModelAttribute("newRole") RoleDto request, RedirectAttributes flash) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Role template name is required.");
            return "redirect:/admin/roles";
        }
        try {
            roleService.createRole(request);
            flash.addFlashAttribute("successMessage", "Role template '" + request.getName().trim() + "' created successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping({"/{roleId:\\d+}/update", "/{roleId:\\d+}/update-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String updateRole(@PathVariable("roleId") Long roleId,
                             @ModelAttribute RoleDto request,
                             RedirectAttributes flash) {
        try {
            roleService.updateRole(roleId, request);
            flash.addFlashAttribute("successMessage", "Role template updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping({"/{roleId:\\d+}/delete", "/{roleId:\\d+}/delete-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String deleteRole(@PathVariable("roleId") Long roleId, RedirectAttributes flash) {
        try {
            roleService.deleteRole(roleId);
            flash.addFlashAttribute("successMessage", "Role template deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
