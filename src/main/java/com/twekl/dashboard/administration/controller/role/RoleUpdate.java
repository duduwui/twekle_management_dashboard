package com.twekl.dashboard.administration.controller.role;

import com.twekl.dashboard.administration.dto.role.RoleDto;
import com.twekl.dashboard.administration.service.role.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Updating Role Templates.
 */
@Controller
@RequestMapping("/admin/roles")
public class RoleUpdate {

    private final RoleService roleService;

    public RoleUpdate(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping({"/{roleId:\\d+}/update", "/{roleId:\\d+}/update-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String updateRole(@PathVariable("roleId") Long roleId,
                             @ModelAttribute RoleDto request,
                             RedirectAttributes flash) {
        try {
            roleService.updateRole(roleId, request);
            flash.addFlashAttribute("successMessage", "Role template updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
