package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterPresetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Managing Timeline & Milestone Filter Presets.
 * Pure Thymeleaf Server-Side Rendering (SSR) with Post-Redirect-Get (PRG) pattern.
 */
@Controller
@RequestMapping("/admin/timelines")
public class TimelinePresetController {

    private final TimeFilterPresetService presetService;
    private final AdminService adminService;

    public TimelinePresetController(TimeFilterPresetService presetService, AdminService adminService) {
        this.presetService = presetService;
        this.adminService = adminService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    @GetMapping
    public String showTimelines(Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "timelines");
        model.addAttribute("presetList", presetService.findAll());
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newPreset")) {
            model.addAttribute("newPreset", new TimeFilterPreset());
        }
        return "administration/timefilter/timeFilters";
    }

    @PostMapping({"/create", "/create-preset"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public String createPreset(@ModelAttribute("newPreset") TimeFilterPreset preset, RedirectAttributes flash) {
        if (preset.getLabelEn() == null || preset.getLabelEn().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Preset name is required.");
            return "redirect:/admin/timelines";
        }
        try {
            presetService.createPreset(preset);
            flash.addFlashAttribute("successMessage", "Time filter preset '" + preset.getLabelEn().trim() + "' created successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }

    @PostMapping({"/{presetId:\\d+}/toggle", "/{presetId:\\d+}/toggle-status"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public String togglePreset(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            TimeFilterPreset toggled = presetService.toggleActive(presetId);
            String status = toggled.isActive() ? "activated" : "deactivated";
            flash.addFlashAttribute("successMessage", "Preset '" + toggled.getLabelEn() + "' " + status + " successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }

    @PostMapping({"/{presetId:\\d+}/delete", "/{presetId:\\d+}/delete-preset"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public String deletePreset(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            presetService.deletePreset(presetId);
            flash.addFlashAttribute("successMessage", "Preset deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }
}

