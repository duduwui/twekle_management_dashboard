package com.twekl.dashboard.controller.administrative;

import com.twekl.dashboard.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.dto.admin.UpdateAdminRequest;
import com.twekl.dashboard.model.admin.Admin;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.service.administrative.AdminService;
import com.twekl.dashboard.service.administrative.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for Staff Administrators and Permissions management.
 * Consolidates SSR view rendering and administrative actions.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private RoleService roleService;

    public AdminController() {
    }

    @Autowired
    public AdminController(AdminService adminService, RoleService roleService) {
        this.adminService = adminService;
        this.roleService = roleService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    // =========================================================
    // SSR VIEW ENDPOINTS
    // =========================================================

    @GetMapping
    public String showAdmins(Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "admins");
        model.addAttribute("adminList", adminService.findAll());
        model.addAttribute("roleList", roleService.findAll());
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newAdmin")) {
            model.addAttribute("newAdmin", new CreateAdminRequest());
        }
        return "administration/admin/admins";
    }

    @PostMapping({"/create_admin", "/create-admin", "/create"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String createAdminSsr(@Valid @ModelAttribute("newAdmin") CreateAdminRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes flash) {
        if (bindingResult.hasErrors()) {
            flash.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/admins";
        }
        try {
            adminService.createAdmin(request);
            flash.addFlashAttribute("successMessage", "Administrator '" + request.getUsername() + "' created successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping({"/{adminId:\\d+}/toggle_status", "/{adminId:\\d+}/toggle-status"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String toggleAdminStatusSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            Admin toggled = adminService.toggleStatus(adminId);
            flash.addFlashAttribute("successMessage", "Administrator '" + toggled.getUsername() + "' status updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping({"/{adminId:\\d+}/delete_admin", "/{adminId:\\d+}/delete-admin"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String deleteAdminSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            adminService.deleteAdmin(adminId);
            flash.addFlashAttribute("successMessage", "Administrator deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    // =========================================================
    // REST API ENDPOINTS
    // =========================================================

    @GetMapping(value = {"", "/api"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ApiResponse<List<Admin>>> getAllAdminsRest() {
        return ResponseEntity.ok(ApiResponse.success(adminService.findAll()));
    }

    @GetMapping(value = "/{adminId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ApiResponse<Admin>> getAdminByIdRest(@PathVariable("adminId") Long adminId) {
        return adminService.findById(adminId)
                .map(admin -> ResponseEntity.ok(ApiResponse.success(admin)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ApiResponse<Admin>> createAdminRest(@Valid @RequestBody CreateAdminRequest request) {
        Admin created = adminService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping(value = "/{adminId:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ApiResponse<Admin>> updateAdminRest(@PathVariable("adminId") Long adminId,
                                                              @RequestBody UpdateAdminRequest request) {
        Admin updated = adminService.updateAdmin(adminId, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PatchMapping(value = {"/{adminId:\\d+}/toggle", "/{adminId:\\d+}/toggle_status", "/{adminId:\\d+}/toggle-status"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ApiResponse<Admin>> toggleAdminStatusRest(@PathVariable("adminId") Long adminId) {
        Admin toggled = adminService.toggleStatus(adminId);
        return ResponseEntity.ok(ApiResponse.success(toggled));
    }

    @DeleteMapping(value = {"/{adminId:\\d+}", "/{adminId:\\d+}/delete_admin", "/{adminId:\\d+}/delete-admin"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public ResponseEntity<ApiResponse<Void>> deleteAdminRest(@PathVariable("adminId") Long adminId) {
        adminService.deleteAdmin(adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
