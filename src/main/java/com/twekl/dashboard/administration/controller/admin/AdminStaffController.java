package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.administration.dto.admin.UpdateAdminRequest;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.role.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Managing Staff Administrators.
 * Pure Thymeleaf Server-Side Rendering (SSR) with Post-Redirect-Get (PRG) pattern.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminStaffController {

    private final AdminService adminService;
    private final RoleService roleService;

    public AdminStaffController(AdminService adminService, RoleService roleService) {
        this.adminService = adminService;
        this.roleService = roleService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

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
