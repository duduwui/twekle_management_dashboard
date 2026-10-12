package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterCreateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Creating Timeline & Milestone Presets.
 */
@Controller
@RequestMapping("/admin/timelines")
public class TimeFilterCreate {

    private final TimeFilterCreateService timeFilterCreateService;

    public TimeFilterCreate(TimeFilterCreateService timeFilterCreateService) {
        this.timeFilterCreateService = timeFilterCreateService;
    }

    @PostMapping({"/create", "/create-preset"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public String createPreset(@ModelAttribute("newPreset") TimeFilterPreset preset, RedirectAttributes flash) {
        if (preset.getLabelEn() == null || preset.getLabelEn().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Preset name is required.");
            return "redirect:/admin/timelines";
        }
        try {
            timeFilterCreateService.createPreset(preset);
            flash.addFlashAttribute("successMessage", "Time filter preset '" + preset.getLabelEn().trim() + "' created successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }
}
