package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.TimeFilterPreset;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller interface contract for Dynamic Time Filter & Milestone Presets.
 * Provides a clean high-level summary of time filter and preset rule actions.
 */
public interface TimeFilterOperations {

    String showTimelines(Model model);

    String createPresetSsr(TimeFilterPreset preset, RedirectAttributes flash);

    String togglePresetSsr(Long presetId, RedirectAttributes flash);

    String deletePresetSsr(Long presetId, RedirectAttributes flash);
}
