package com.twekl.dashboard.administration.controller.admin;

import com.twekl.dashboard.administration.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.administration.service.admin.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Creating Staff Administrators.
 */
@Controller
@RequestMapping("/admin/admins")
public class AdminCreate {

    private final AdminService adminService;

    public AdminCreate(AdminService adminService) {
        this.adminService = adminService;
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
}
