package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller for Staff Administrators and Permissions management.
 * Consolidates SSR view rendering and administrative actions.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminController {

    private final AdminService adminService;
    private final RoleService roleService;

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

    @PostMapping("/create-admin")
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

    @PostMapping("/{adminId:\\d+}/toggle-status")
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

    @PostMapping("/{adminId:\\d+}/delete-admin")
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
}
