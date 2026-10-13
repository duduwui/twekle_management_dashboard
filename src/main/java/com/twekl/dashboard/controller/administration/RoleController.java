package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.UpdateAdminRequest;
import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.model.administration.Role;
import com.twekl.dashboard.response.administration.ApiResponse;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for Administrative Roles and Permissions assignment.
 * Consolidates SSR view rendering and REST role actions.
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

    // =========================================================
    // SSR VIEW ENDPOINTS
    // =========================================================

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
            selectedAdmin = adminService.findById(selectedAdminId).orElse(null);
        } else if (!adminList.isEmpty()) {
            selectedAdmin = adminList.get(0);
        }
        model.addAttribute("selectedAdmin", selectedAdmin);

        if (!model.containsAttribute("newRole")) {
            model.addAttribute("newRole", new RoleDto());
        }
        return "administration/role/roles";
    }

    @PostMapping({"/create_role", "/create", "/create-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String createRoleSsr(@ModelAttribute("newRole") RoleDto request, RedirectAttributes flash) {
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

    @PostMapping({"/{roleId:\\d+}/update_role", "/{roleId:\\d+}/update", "/{roleId:\\d+}/update-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String updateRoleSsr(@PathVariable("roleId") Long roleId,
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

    @PostMapping({"/{adminId:\\d+}/assign_user_role", "/{adminId:\\d+}/assign", "/assign", "/assign_user_role", "/assign-user-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS', 'PERMISSION_CREATE_ROLES')")
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

        try {
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
            flash.addFlashAttribute("successMessage", "Assigned permissions updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles?selectedAdminId=" + targetAdminId;
    }

    @PostMapping({"/{roleId:\\d+}/delete_role", "/{roleId:\\d+}/delete", "/{roleId:\\d+}/delete-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String deleteRoleSsr(@PathVariable("roleId") Long roleId, RedirectAttributes flash) {
        try {
            roleService.deleteRole(roleId);
            flash.addFlashAttribute("successMessage", "Role template deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }

    // =========================================================
    // REST API ENDPOINTS
    // =========================================================

    @GetMapping(value = {"", "/api"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRolesRest() {
        return ResponseEntity.ok(ApiResponse.success(roleService.findAll()));
    }

    @GetMapping(value = "/{roleId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public ResponseEntity<ApiResponse<Role>> getRoleByIdRest(@PathVariable("roleId") Long roleId) {
        return roleService.findById(roleId)
                .map(role -> ResponseEntity.ok(ApiResponse.success(role)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public ResponseEntity<ApiResponse<Role>> createRoleRest(@Valid @RequestBody RoleDto request) {
        Role created = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping(value = "/{roleId:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public ResponseEntity<ApiResponse<Role>> updateRoleRest(@PathVariable("roleId") Long roleId,
                                                            @Valid @RequestBody RoleDto request) {
        Role updated = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping(value = "/{roleId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public ResponseEntity<ApiResponse<Void>> deleteRoleRest(@PathVariable("roleId") Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
