package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.role.RoleService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for Viewing Staff Administrators Directory.
 * Pure Thymeleaf Server-Side Rendering (SSR).
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminViewController {

    private final AdminService adminService;
    private final RoleService roleService;

    public AdminViewController(AdminService adminService, RoleService roleService) {
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
}
