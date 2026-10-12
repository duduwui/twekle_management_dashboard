package com.twekl.dashboard.administration.controller.role;

import com.twekl.dashboard.administration.dto.role.RoleDto;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.role.RoleService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Roles and Permission Matrix Directory.
 */
@Controller
@RequestMapping("/admin/roles")
public class RoleList {

    private final RoleService roleService;
    private final AdminService adminService;

    public RoleList(RoleService roleService, AdminService adminService) {
        this.roleService = roleService;
        this.adminService = adminService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    @GetMapping
    public String showRoles(@RequestParam(value = "selectedAdminId", required = false) Long selectedAdminId,
                            Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "roles");
        List<Admin> adminList = adminService.findAll();
        List<Role> roleList = roleService.findAll();
        model.addAttribute("adminList", adminList);
        model.addAttribute("roleList", roleList);
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));

        Admin selectedAdmin = null;
        if (selectedAdminId != null) {
            selectedAdmin = adminList.stream().filter(a -> a.getAdminId().equals(selectedAdminId)).findFirst().orElse(null);
        }
        if (selectedAdmin == null && !adminList.isEmpty()) {
            selectedAdmin = adminList.get(0);
        }
        model.addAttribute("selectedAdmin", selectedAdmin);

        if (!model.containsAttribute("newRole")) {
            model.addAttribute("newRole", new RoleDto());
        }
        return "administration/role/roles";
    }
}
