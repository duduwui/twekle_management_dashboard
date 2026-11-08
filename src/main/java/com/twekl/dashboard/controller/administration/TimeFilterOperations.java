package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.TimeFilterPreset;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ============================================================================
 * Time Filter Controller Operations Contract (TimeFilterOperations.java)
 * ============================================================================
 * 
 * Defines all Web SSR endpoints for managing dynamic timeline filter presets,
 * intervals, and follow-up milestone configurations.
 * 
 * Canonical Route: /admin/timelines
 * 
 * Core Capabilities:
 *   1. Presets View: SSR display of configured timeline milestone intervals.
 *   2. Preset Registration: Addition of custom time milestones (e.g. 24 Hours, 3 Days).
 *   3. Status Toggle: Activates or deactivates presets, initiating background check re-syncs.
 *   4. Safe Deletion: Removes obsolete presets and updates pending checks.
 * ============================================================================
 */
public interface TimeFilterOperations {

    /**
     * Renders the dynamic timeline presets and filter intervals SSR HTML page.
     *
     * @param model Spring MVC Model container.
     * @return Thymeleaf view path: "administration/timefilter/timeFilters".
     */
    String showTimelines(Model model);

    /**
     * Creates a new dynamic time filter and milestone preset.
     *
     * @param preset TimeFilterPreset entity to create.
     * @param flash  RedirectAttributes container for flash messaging.
     * @return Redirect to "/admin/timelines".
     */
    String createPresetSsr(TimeFilterPreset preset, RedirectAttributes flash);

    /**
     * Toggles a preset's active state (active <-> inactive).
     *
     * @param presetId ID of the preset to toggle.
     * @param request  HttpServletRequest container for inspecting AJAX headers.
     * @param flash    RedirectAttributes container.
     * @return Redirect to "/admin/timelines" or JSON response for AJAX.
     */
    Object togglePresetSsr(Long presetId, HttpServletRequest request, RedirectAttributes flash);

    /**
     * Deletes a preset configuration from the system.
     *
     * @param presetId ID of the preset to delete.
     * @param request  HttpServletRequest container for inspecting AJAX headers.
     * @param flash    RedirectAttributes container.
     * @return Redirect to "/admin/timelines" or JSON response for AJAX.
     */
    Object deletePresetSsr(Long presetId, HttpServletRequest request, RedirectAttributes flash);
}
