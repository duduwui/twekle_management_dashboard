package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Primary Administrative Controller.
 * Consolidates SSR view rendering and administrative actions.
 * Uses constructor injection with final dependencies.
 */
@Controller
public class AdminController {

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

    @PostMapping("/admin/admins/{adminId:\\d+}/toggle-status")
    public String toggleAdminStatusSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        if (!portalAccess.canManageStaff()) {
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to toggle administrator status.");
            return "redirect:/admin/admins";
        }
        return executeAdminAction(flash, "Administrator status updated successfully.", 
                () -> adminService.toggleStatus(adminId));
    }

    @PostMapping("/admin/admins/{adminId:\\d+}/delete-admin")
    public String deleteAdminSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        if (!portalAccess.canManageStaff()) {
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to delete administrators.");
            return "redirect:/admin/admins";
        }
        return executeAdminAction(flash, "Administrator deleted successfully.", 
                () -> adminService.deleteAdmin(adminId));
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
