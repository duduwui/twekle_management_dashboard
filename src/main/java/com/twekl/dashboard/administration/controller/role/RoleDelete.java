package com.twekl.dashboard.administration.controller.role;

import com.twekl.dashboard.administration.service.role.RoleDeleteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Deleting Role Templates.
 */
@Controller
@RequestMapping("/admin/roles")
public class RoleDelete {

    private final RoleDeleteService roleDeleteService;

    public RoleDelete(RoleDeleteService roleDeleteService) {
        this.roleDeleteService = roleDeleteService;
    }

    @PostMapping({"/{roleId:\\d+}/delete", "/{roleId:\\d+}/delete-role"})
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CREATE_ROLES')")
    public String deleteRole(@PathVariable("roleId") Long roleId, RedirectAttributes flash) {
        try {
            roleDeleteService.deleteRole(roleId);
            flash.addFlashAttribute("successMessage", "Role template deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
