package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.dto.administration.AdminUserDto;
import com.twekl.dashboard.dto.administration.RoleDto;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.RoleService;
import com.twekl.dashboard.model.administration.Admin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Primary Administrative Controller.
 * Consolidates SSR view rendering and administrative actions.
 * Implements AdminOperations interface contract.
 * Uses constructor injection with final dependencies.
 */
@Controller
public class AdminController implements AdminOperations {

    private final AdminService adminService;
    private final RoleService roleService;
    private final com.twekl.dashboard.security.administration.PortalAccess portalAccess;

    @Autowired
    public AdminController(final AdminService adminService,
                           final RoleService roleService,
                           final com.twekl.dashboard.security.administration.PortalAccess portalAccess) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.portalAccess = portalAccess;
    }

    // =========================================================
    // STAFF ADMINISTRATORS ENDPOINTS
    // =========================================================

    @Override
    @GetMapping("/admin/admins")
    public String showAdmins(Model model) {
        if (!portalAccess.canViewStaff()) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("activeTab", "admins");
        model.addAttribute("admins", adminService.findAll());
        model.addAttribute("roles", roleService.findAll());
        if (!model.containsAttribute("newAdmin")) {
            model.addAttribute("newAdmin", new CreateAdminRequest());
        }
        return "administration/admin/admins";
    }

    @Override
    @GetMapping(value = {"/admin/admins/json", "/admin/api/admins", "/admin/admins/api"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminsJson() {
        if (!portalAccess.canViewStaff()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not have permission to view administrators."));
        }
        List<AdminUserDto> dtos = adminService.findAll().stream()
                .map(AdminUserDto::fromEntity)
                .toList();

        List<RoleDto> roleDtos = roleService.findAll().stream()
                .map(r -> new RoleDto(r.getId(), r.getName(), r.isCanCreate(), r.isCanRead(), r.isCanUpdate(), r.isCanDelete(), r.getActions()))
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCount", dtos.size());
        data.put("admins", dtos);
        data.put("users", dtos);
        data.put("roles", roleDtos);

        return ResponseEntity.ok(ApiResponse.success("Administrators and roles retrieved successfully", data));
    }

    @Override
    @PostMapping("/admin/admins/create-admin")
    public String createAdminSsr(@Valid @ModelAttribute("newAdmin") CreateAdminRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes flash) {
        if (!portalAccess.canManageStaff()) {
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to create administrators.");
            return "redirect:/admin/admins";
        }
        if (bindingResult.hasErrors()) {
            flash.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/admins";
        }
        return executeAdminAction(flash, "Administrator '" + request.getUsername() + "' created successfully.", 
                () -> adminService.createAdmin(request));
    }

    @Override
    @PostMapping({"/admin/admins/{adminId:\\d+}/toggle-status", "/admin/roles/{adminId:\\d+}/toggle-status"})
    public Object toggleAdminStatusSsr(@PathVariable("adminId") Long adminId,
                                       @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                       HttpServletRequest request,
                                       RedirectAttributes flash) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (!portalAccess.canManageStaff() && !portalAccess.canEditPermissions()) {
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Access denied: You do not have permission to toggle administrator status."));
            }
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to toggle administrator status.");
            return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/admin/admins";
        }

        try {
            Admin updated = adminService.toggleStatus(adminId);
            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "active", updated.isActive(),
                        "adminId", adminId,
                        "message", "Administrator status updated to " + (updated.isActive() ? "Active" : "Inactive") + "."
                ));
            }
            flash.addFlashAttribute("successMessage", "Administrator status updated successfully.");
        } catch (Exception ex) {
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : "Failed to toggle administrator status.";
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", errorMsg
                ));
            }
            flash.addFlashAttribute("errorMessage", errorMsg);
        }

        return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/admin/admins";
    }

    @Override
    @PostMapping("/admin/admins/{adminId:\\d+}/delete-admin")
    public Object deleteAdminSsr(@PathVariable("adminId") Long adminId,
                                 HttpServletRequest request,
                                 RedirectAttributes flash) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (!portalAccess.canManageStaff()) {
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Access denied: You do not have permission to delete administrators."));
            }
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to delete administrators.");
            return "redirect:/admin/admins";
        }

        try {
            adminService.deleteAdmin(adminId);
            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "adminId", adminId,
                        "message", "Administrator deleted successfully."
                ));
            }
            flash.addFlashAttribute("successMessage", "Administrator deleted successfully.");
        } catch (Exception ex) {
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", ex.getMessage()));
            }
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
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
    private String executeAdminAction(RedirectAttributes flash, String successMessage, ActionCallback action) {
        try {
            action.execute();
            flash.addFlashAttribute("successMessage", successMessage);
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
