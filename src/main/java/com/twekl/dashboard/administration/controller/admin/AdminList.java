package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.administration.service.admin.AdminQueryService;
import com.twekl.dashboard.administration.service.role.RoleQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Staff Administrators Directory.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminList {

    private final AdminQueryService adminQueryService;
    private final RoleQueryService roleQueryService;

    public AdminList(AdminQueryService adminQueryService, RoleQueryService roleQueryService) {
        this.adminQueryService = adminQueryService;
        this.roleQueryService = roleQueryService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    @GetMapping
    public String showAdmins(Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "admins");
        model.addAttribute("adminList", adminQueryService.findAll());
        model.addAttribute("roleList", roleQueryService.findAll());
        model.addAttribute("currentUser", adminQueryService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newAdmin")) {
            model.addAttribute("newAdmin", new CreateAdminRequest());
        }
        return "administration/admin/admins";
    }
}
