package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.administration.dto.admin.UpdateAdminRequest;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.service.admin.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Mutating Staff Administrators (Create, Toggle, Update Permissions, Delete).
 * Follows Post-Redirect-Get (PRG) pattern with flash attributes.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminActionController {

    private final AdminService adminService;

    public AdminActionController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/create-admin")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String createAdmin(@Valid @ModelAttribute("newAdmin") CreateAdminRequest request,
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
    public String toggleAdminStatus(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            Admin toggled = adminService.toggleStatus(adminId);
            String status = toggled.isActive() ? "activated" : "deactivated";
            flash.addFlashAttribute("successMessage", "Administrator '" + toggled.getUsername() + "' " + status + " successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping({"/{adminId:\\d+}/update-permissions", "/{adminId:\\d+}/update-admin"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String updateAdminPermissions(@PathVariable("adminId") Long adminId,
                                         @ModelAttribute UpdateAdminRequest request,
                                         RedirectAttributes flash) {
        try {
            adminService.updateAdmin(adminId, request);
            flash.addFlashAttribute("successMessage", "Permissions updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping("/{adminId:\\d+}/delete-admin")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String deleteAdmin(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            adminService.deleteAdmin(adminId);
            flash.addFlashAttribute("successMessage", "Administrator deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
