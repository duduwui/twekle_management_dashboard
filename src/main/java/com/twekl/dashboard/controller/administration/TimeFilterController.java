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
 * Implements TimeFilterOperations interface contract.
 */
@Controller
public class TimeFilterController implements TimeFilterOperations {

    private final TimeFilterService timeFilterService;
    private final AdminService adminService;

    @Autowired
    public TimeFilterController(final TimeFilterService timeFilterService, final AdminService adminService) {
        this.timeFilterService = timeFilterService;
        this.adminService = adminService;
    }

    // =========================================================
    // SSR VIEW ENDPOINTS (/admin/timelines)
    // =========================================================

    @Override
    @GetMapping(value = "/admin/timelines", produces = MediaType.TEXT_HTML_VALUE)
    public String showTimelines(Model model) {
        model.addAttribute("activeTab", "timelines");
        var presets = timeFilterService.findAll();
        model.addAttribute("presets", presets);
        model.addAttribute("presetList", presets);
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newPreset")) {
            model.addAttribute("newPreset", new TimeFilterPreset());
        }
        return "administration/timefilter/timeFilters";
    }

    @Override
    @PostMapping("/admin/timelines/create")
    public String createPresetSsr(@ModelAttribute("newPreset") TimeFilterPreset preset, RedirectAttributes flash) {
        if (preset.getLabelEn() == null || preset.getLabelEn().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Preset name is required.");
            return "redirect:/admin/timelines";
        }
        return executeTimeFilterAction(flash, "Time filter preset '" + preset.getLabelEn().trim() + "' created successfully.",
                () -> timeFilterService.createPreset(preset));
    }

    @Override
    @PostMapping("/admin/timelines/{presetId:\\d+}/toggle")
    public String togglePresetSsr(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        return executeTimeFilterAction(flash, "Preset status updated successfully.",
                () -> timeFilterService.toggleStatus(presetId));
    }

    @Override
    @PostMapping("/admin/timelines/{presetId:\\d+}/delete")
    public String deletePresetSsr(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        return executeTimeFilterAction(flash, "Time filter preset deleted successfully.",
                () -> timeFilterService.deletePreset(presetId));
    }

    // =========================================================
    // REUSABLE HELPER ENGINE
    // =========================================================

    /**
     * Reusable logic block that manages try-catch boundaries and routing dynamically.
     * Consolidates error feedback pipelines into one shared engine.
     */
    private String executeTimeFilterAction(RedirectAttributes flash, String successMessage, ControllerAction action) {
        try {
            action.execute();
            flash.addFlashAttribute("successMessage", successMessage);
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }
}
