package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.dto.admin.UpdateAdminRequest;
import com.twekl.dashboard.administration.service.admin.AdminUpdateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Updating Administrator Permissions.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminUpdate {

    private final AdminUpdateService adminUpdateService;

    public AdminUpdate(AdminUpdateService adminUpdateService) {
        this.adminUpdateService = adminUpdateService;
    }

    @PostMapping({"/{adminId:\\d+}/update-permissions", "/{adminId:\\d+}/update-admin"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_MANAGE_USERS')")
    public String updateAdminPermissions(@PathVariable("adminId") Long adminId,
                                         @ModelAttribute UpdateAdminRequest request,
                                         RedirectAttributes flash) {
        try {
            adminUpdateService.updateAdmin(adminId, request);
            flash.addFlashAttribute("successMessage", "Permissions updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
