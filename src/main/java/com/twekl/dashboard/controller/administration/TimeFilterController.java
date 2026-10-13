package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.response.administration.ApiResponse;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.TimeFilterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller for Dynamic Time Filter & Milestone Presets.
 * Consolidates SSR view rendering and REST endpoints.
 */
@Controller
public class TimeFilterController {

    @Autowired
    private TimeFilterService timeFilterService;

    @Autowired
    private AdminService adminService;

    public TimeFilterController() {
    }

    @Autowired
    public TimeFilterController(TimeFilterService timeFilterService, AdminService adminService) {
        this.timeFilterService = timeFilterService;
        this.adminService = adminService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    // =========================================================
    // SSR VIEW ENDPOINTS (/admin/timelines)
    // =========================================================

    @GetMapping(value = "/admin/timelines", produces = MediaType.TEXT_HTML_VALUE)
    public String showTimelines(Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "timelines");
        model.addAttribute("presetList", timeFilterService.findAll());
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newPreset")) {
            model.addAttribute("newPreset", new TimeFilterPreset());
        }
        return "administration/timefilter/timeFilters";
    }

    @PostMapping({"/admin/timelines/create_preset", "/admin/timelines/create", "/admin/timelines/create-preset"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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

    @PostMapping({"/admin/timelines/{presetId:\\d+}/toggle_status", "/admin/timelines/{presetId:\\d+}/toggle", "/admin/timelines/{presetId:\\d+}/toggle-status"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public String togglePresetSsr(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            TimeFilterPreset toggled = timeFilterService.toggleStatus(presetId);
            flash.addFlashAttribute("successMessage", "Preset '" + toggled.getLabelEn() + "' status updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }

    @PostMapping({"/admin/timelines/{presetId:\\d+}/delete_preset", "/admin/timelines/{presetId:\\d+}/delete", "/admin/timelines/{presetId:\\d+}/delete-preset"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public String deletePresetSsr(@PathVariable("presetId") Long presetId, RedirectAttributes flash) {
        try {
            timeFilterService.deletePreset(presetId);
            flash.addFlashAttribute("successMessage", "Preset deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/timelines";
    }

    // =========================================================
    // REST API ENDPOINTS (/admin/time-filters)
    // =========================================================

    @GetMapping(value = {"/admin/time_filters", "/admin/time-filters", "/admin/presets"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TimeFilterPreset>>> getAllPresetsRest() {
        return ResponseEntity.ok(ApiResponse.success(timeFilterService.findAll()));
    }

    @GetMapping(value = {"/admin/time_filters/active", "/admin/time-filters/active"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TimeFilterPreset>>> getActivePresetsRest() {
        return ResponseEntity.ok(ApiResponse.success(timeFilterService.findActive()));
    }

    @GetMapping(value = {"/admin/time_filters/{presetId:\\d+}", "/admin/time-filters/{presetId:\\d+}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> getPresetByIdRest(@PathVariable("presetId") Long presetId) {
        return timeFilterService.findById(presetId)
                .map(preset -> ResponseEntity.ok(ApiResponse.success(preset)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = {"/admin/time_filters", "/admin/time-filters"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> createPresetRest(@Valid @RequestBody TimeFilterPreset preset) {
        TimeFilterPreset created = timeFilterService.createPreset(preset);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping(value = {"/admin/time_filters/{presetId:\\d+}", "/admin/time-filters/{presetId:\\d+}"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> updatePresetRest(@PathVariable("presetId") Long presetId,
                                                                         @Valid @RequestBody TimeFilterPreset preset) {
        TimeFilterPreset updated = timeFilterService.updatePreset(presetId, preset);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PatchMapping(value = {"/admin/time_filters/{presetId:\\d+}/toggle_status", "/admin/time_filters/{presetId:\\d+}/toggle", "/admin/time-filters/{presetId:\\d+}/toggle", "/admin/time-filters/{presetId:\\d+}/toggle-status"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> togglePresetRest(@PathVariable("presetId") Long presetId) {
        TimeFilterPreset toggled = timeFilterService.toggleStatus(presetId);
        return ResponseEntity.ok(ApiResponse.success(toggled));
    }

    @DeleteMapping(value = {"/admin/time_filters/{presetId:\\d+}", "/admin/time-filters/{presetId:\\d+}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePresetRest(@PathVariable("presetId") Long presetId) {
        timeFilterService.deletePreset(presetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
