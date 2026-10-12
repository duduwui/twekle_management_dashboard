package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.service.admin.AdminStatusService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Toggling Administrator Active Status.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminStatus {

    private final AdminStatusService adminStatusService;

    public AdminStatus(AdminStatusService adminStatusService) {
        this.adminStatusService = adminStatusService;
    }

    @PostMapping("/{adminId:\\d+}/toggle-status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String toggleAdminStatus(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            Admin toggled = adminStatusService.toggleStatus(adminId);
            flash.addFlashAttribute("successMessage", "Administrator '" + toggled.getUsername() + "' status updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
