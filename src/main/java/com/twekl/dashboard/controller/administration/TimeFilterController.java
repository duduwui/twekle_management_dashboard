package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.TimeFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Dynamic Time Filter & Milestone Presets.
 * Pure SSR view rendering and administrative actions.
 */
@Controller
public class TimeFilterController {

    private final TimeFilterService timeFilterService;
    private final AdminService adminService;

    @Autowired
    public TimeFilterController(TimeFilterService timeFilterService, AdminService adminService) {
        this.timeFilterService = timeFilterService;
        this.adminService = adminService;
    }

    // =========================================================
    // SSR VIEW ENDPOINTS (/admin/timelines)
    // =========================================================

    @GetMapping(value = "/admin/timelines", produces = MediaType.TEXT_HTML_VALUE)
    public String showTimelines(Model model) {
        model.addAttribute("activeTab", "timelines");
        model.addAttribute("presetList", timeFilterService.findAll());
        model.addAttribute("presets", timeFilterService.findAll());
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newPreset")) {
            model.addAttribute("newPreset", new TimeFilterPreset());
        }
        return "administration/timefilter/timeFilters";
    }

    @PostMapping({"/admin/timelines/create", "/admin/timelines/create_preset"})
    public String createPresetSsr(@ModelAttribute("newPreset") TimeFilterPreset preset, RedirectAttributes flash) {
        if (preset.getLabelEn() == null || preset.getLabelEn().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Preset name is required.");
            return "redirect:/admin/timelines";
        }
        try {
            timeFilterService.createPreset(preset);
            flash.addFlashAttribute("successMessage", "Time filter preset '" + preset.getLabelEn().trim() + "' created successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }

    @PostMapping({"/admin/timelines/{presetId:\\d+}/toggle", "/admin/timelines/{presetId:\\d+}/toggle_status"})
    public String togglePresetSsr(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            TimeFilterPreset toggled = timeFilterService.toggleStatus(presetId);
            flash.addFlashAttribute("successMessage", "Preset '" + toggled.getLabelEn() + "' status updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }

    @PostMapping({"/admin/timelines/{presetId:\\d+}/delete", "/admin/timelines/{presetId:\\d+}/delete_preset"})
    public String deletePresetSsr(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            timeFilterService.deletePreset(presetId);
            flash.addFlashAttribute("successMessage", "Preset deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }
}
