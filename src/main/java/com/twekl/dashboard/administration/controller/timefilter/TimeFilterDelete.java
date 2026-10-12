package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.service.timefilter.TimeFilterPresetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Deleting Timeline Presets.
 */
@Controller
@RequestMapping("/admin/timelines")
public class TimeFilterDelete {

    private final TimeFilterPresetService presetService;

    public TimeFilterDelete(TimeFilterPresetService presetService) {
        this.presetService = presetService;
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
