package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.TimeFilterService;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Dynamic Time Filter & Milestone Presets. Renders milestone rules and handles their
 * form submissions.
 */
@Controller
@PreAuthorize("@portalAccess.allows('TIMELINES_PRESETS','READ')")
public class TimeFilterController {

    private final TimeFilterService timeFilterService;

    private final AdminService adminService;

    public TimeFilterController(TimeFilterService timeFilterService, AdminService adminService) {
        this.timeFilterService = timeFilterService;
        this.adminService = adminService;
    }

    @InitBinder("newPreset")
    public void bindPreset(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields(
                "labelEn",
                "labelAr",
                "labelKu",
                "durationValue",
                "durationUnit",
                "displayOrder",
                "active");
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    // SSR VIEW ENDPOINTS (/admin/timelines)

    @GetMapping(value = "/admin/timelines", produces = MediaType.TEXT_HTML_VALUE)
    public String showTimelines(@RequestParam(defaultValue = "0") int page, Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "timelines");
        var rows = timeFilterService.findPage(page);
        model.addAttribute("rows", rows);
        model.addAttribute("presetList", rows.getContent());
        if (!model.containsAttribute("newPreset")) {
            model.addAttribute("newPreset", new TimeFilterPreset());
        }
        return "administration/timefilter/timeFilters";
    }

    @PostMapping({
        "/admin/timelines/create_preset",
        "/admin/timelines/create",
        "/admin/timelines/create-preset"
    })
    @PreAuthorize("@portalAccess.allows('TIMELINES_PRESETS','CREATE')")
    public String createPresetSsr(
            @ModelAttribute("newPreset") TimeFilterPreset preset, RedirectAttributes flash) {
        if (preset.getLabelEn() == null || preset.getLabelEn().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Preset name is required.");
            return "redirect:/admin/timelines";
        }
        try {
            timeFilterService.createPreset(preset);
            flash.addFlashAttribute(
                    "successMessage",
                    "Time filter preset '"
                            + preset.getLabelEn().trim()
                            + "' created successfully.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }

    @PostMapping({
        "/admin/timelines/{presetId:\\d+}/toggle_status",
        "/admin/timelines/{presetId:\\d+}/toggle",
        "/admin/timelines/{presetId:\\d+}/toggle-status"
    })
    @PreAuthorize("@portalAccess.allows('TIMELINES_PRESETS','UPDATE')")
    public String togglePresetSsr(
            @PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            TimeFilterPreset toggled = timeFilterService.toggleStatus(presetId);
            flash.addFlashAttribute(
                    "successMessage",
                    "Preset '" + toggled.getLabelEn() + "' status updated successfully.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }

    @PostMapping({
        "/admin/timelines/{presetId:\\d+}/delete_preset",
        "/admin/timelines/{presetId:\\d+}/delete",
        "/admin/timelines/{presetId:\\d+}/delete-preset"
    })
    @PreAuthorize("@portalAccess.allows('TIMELINES_PRESETS','DELETE')")
    public String deletePresetSsr(
            @PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            timeFilterService.deletePreset(presetId);
            flash.addFlashAttribute("successMessage", "Preset deleted successfully.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }
}
