package com.twekl.dashboard.administration.controller.role;

import com.twekl.dashboard.administration.dto.role.RoleDto;
import com.twekl.dashboard.administration.service.role.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Creating Role Templates.
 */
@Controller
@RequestMapping("/admin/roles")
public class RoleCreateController {

    private final RoleService roleService;

    public RoleCreateController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping({"/create", "/create-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String createRole(@ModelAttribute("newRole") RoleDto request, RedirectAttributes flash) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Role template name is required.");
            return "redirect:/admin/roles";
        }
        try {
            roleService.createRole(request);
            flash.addFlashAttribute("successMessage", "Role template '" + request.getName().trim() + "' created successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
