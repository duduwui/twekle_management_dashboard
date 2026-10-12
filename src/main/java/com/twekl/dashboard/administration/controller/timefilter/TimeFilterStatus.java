package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterStatusService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Toggling Timeline Preset Status.
 */
@Controller
@RequestMapping("/admin/timelines")
public class TimeFilterStatus {

    private final TimeFilterStatusService timeFilterStatusService;

    public TimeFilterStatus(TimeFilterStatusService timeFilterStatusService) {
        this.timeFilterStatusService = timeFilterStatusService;
    }

    @PostMapping({"/{presetId:\\d+}/toggle", "/{presetId:\\d+}/toggle-status"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public String togglePreset(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            TimeFilterPreset toggled = timeFilterStatusService.toggleActive(presetId);
            flash.addFlashAttribute("successMessage", "Preset '" + toggled.getLabelEn() + "' status updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }
}
