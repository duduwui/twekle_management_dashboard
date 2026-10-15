package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Primary Administrative Controller.
 * Consolidates SSR view rendering and administrative actions.
 * Implements AdminOperations interface contract.
 * Uses constructor injection with final dependencies.
 */
@Controller
public class AdminController implements AdminOperations {

    private final AdminService adminService;
    private final RoleService roleService;

    @Autowired
    public AdminController(final AdminService adminService,
                           final RoleService roleService) {
        this.adminService = adminService;
        this.roleService = roleService;
    }

    // =========================================================
    // STAFF ADMINISTRATORS ENDPOINTS (AdminOperations Contract)
    // =========================================================

    @Override
    @GetMapping("/admin/admins")
    public String showAdmins(Model model) {
        model.addAttribute("activeTab", "admins");
        model.addAttribute("admins", adminService.findAll());
        model.addAttribute("roles", roleService.findAll());
        if (!model.containsAttribute("newAdmin")) {
            model.addAttribute("newAdmin", new CreateAdminRequest());
        }
        return "administration/admin/admins";
    }

    @Override
    @PostMapping("/admin/admins/create-admin")
    public String createAdminSsr(@Valid @ModelAttribute("newAdmin") CreateAdminRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes flash) {
        if (bindingResult.hasErrors()) {
            flash.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/admins";
        }
        return executeAdminAction(flash, "Administrator '" + request.getUsername() + "' created successfully.", 
                () -> adminService.createAdmin(request));
    }

    @Override
    @PostMapping("/admin/admins/{adminId:\\d+}/toggle-status")
    public String toggleAdminStatusSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        return executeAdminAction(flash, "Administrator status updated successfully.", 
                () -> adminService.toggleStatus(adminId));
    }

    @Override
    @PostMapping("/admin/admins/{adminId:\\d+}/delete-admin")
    public String deleteAdminSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        return executeAdminAction(flash, "Administrator deleted successfully.", 
                () -> adminService.deleteAdmin(adminId));
    }

    // =========================================================
    // REUSABLE HELPER ENGINE
    // =========================================================

    /**
     * Reusable logic block that manages try-catch boundaries and routing dynamically.
     * Consolidates error feedback pipelines into one shared engine.
     */
    private String executeAdminAction(RedirectAttributes flash, String successMessage, Action action) {
        try {
            action.execute();
            flash.addFlashAttribute("successMessage", successMessage);
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
