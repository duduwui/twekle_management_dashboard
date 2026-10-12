package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.service.admin.AdminDeleteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Deleting Staff Administrators.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminDelete {

    private final AdminDeleteService adminDeleteService;

    public AdminDelete(AdminDeleteService adminDeleteService) {
        this.adminDeleteService = adminDeleteService;
    }

    @PostMapping("/{adminId:\\d+}/delete-admin")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String deleteAdmin(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            adminDeleteService.deleteAdmin(adminId);
            flash.addFlashAttribute("successMessage", "Administrator deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
