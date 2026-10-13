package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.service.administration.AdminService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Staff Administrators and Permissions management. Consolidates SSR view rendering
 * and administrative actions.
 */
@Controller
@PreAuthorize("@portalAccess.allows('ADMIN_MANAGEMENT','READ')")
@RequestMapping("/admin/admins")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @InitBinder("newAdmin")
    public void bindAdmin(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("username", "password", "phoneNumber", "active", "superAdmin");
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    // SSR VIEW ENDPOINTS

    @GetMapping
    public String showAdmins(@RequestParam(defaultValue = "0") int page, Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "admins");
        var rows = adminService.findPage(page);
        model.addAttribute("rows", rows);
        model.addAttribute("adminList", rows.getContent());
        if (!model.containsAttribute("newAdmin")) {
            model.addAttribute("newAdmin", new CreateAdminRequest());
        }
        return "administration/admin/admins";
    }

    @PostMapping({"/create_admin", "/create-admin", "/create"})
    @PreAuthorize("@portalAccess.allows('ADMIN_MANAGEMENT','CREATE')")
    public String createAdminSsr(
            @Valid @ModelAttribute("newAdmin") CreateAdminRequest request,
            BindingResult bindingResult,
            RedirectAttributes flash) {
        if (bindingResult.hasErrors()) {
            flash.addFlashAttribute(
                    "errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/admins";
        }
        if (request.isSuperAdmin()
                && !adminService
                        .getCurrentAdmin()
                        .map(com.twekl.dashboard.model.administration.Admin::isSuperAdmin)
                        .orElse(false)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only a super administrator can grant super access");
        }
        try {
            adminService.createAdmin(request);
            flash.addFlashAttribute(
                    "successMessage",
                    "Administrator '" + request.getUsername() + "' created successfully.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping({"/{adminId:\\d+}/toggle_status", "/{adminId:\\d+}/toggle-status"})
    @PreAuthorize("@portalAccess.allows('ADMIN_MANAGEMENT','UPDATE')")
    public String toggleAdminStatusSsr(
            @PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            Admin toggled = adminService.toggleStatus(adminId);
            flash.addFlashAttribute(
                    "successMessage",
                    "Administrator '" + toggled.getUsername() + "' status updated successfully.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping({"/{adminId:\\d+}/delete_admin", "/{adminId:\\d+}/delete-admin"})
    @PreAuthorize("@portalAccess.allows('ADMIN_MANAGEMENT','DELETE')")
    public String deleteAdminSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            adminService.deleteAdmin(adminId);
            flash.addFlashAttribute("successMessage", "Administrator deleted successfully.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
