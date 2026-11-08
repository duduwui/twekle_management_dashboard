package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.TimeFilterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
/**
 * Controller for Dynamic Time Filter & Milestone Presets.
 * Pure SSR view rendering and administrative actions.
 * Implements TimeFilterOperations interface contract.
 */
@Controller
public class TimeFilterController implements TimeFilterOperations {

    private final TimeFilterService timeFilterService;
    private final AdminService adminService;
    private final com.twekl.dashboard.security.administration.PortalAccess portalAccess;

    @Autowired
    public TimeFilterController(final TimeFilterService timeFilterService,
                                final AdminService adminService,
                                final com.twekl.dashboard.security.administration.PortalAccess portalAccess) {
        this.timeFilterService = timeFilterService;
        this.adminService = adminService;
        this.portalAccess = portalAccess;
    }

    // =========================================================
    // SSR VIEW ENDPOINTS (/admin/time-filters and /admin/timelines)
    // =========================================================

    @Override
    @GetMapping(value = {"/admin/time-filters", "/admin/timelines"}, produces = MediaType.TEXT_HTML_VALUE)
    public String showTimelines(Model model) {
        if (!portalAccess.canViewPresets()) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("activeTab", "time-filters");
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
    @PostMapping({"/admin/time-filters/create", "/admin/timelines/create"})
    public String createPresetSsr(@ModelAttribute("newPreset") TimeFilterPreset preset, RedirectAttributes flash) {
        if (!portalAccess.canManagePresets()) {
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to manage time filters.");
            return getRedirectPath();
        }
        if (preset.getLabelEn() == null || preset.getLabelEn().trim().isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Preset name is required.");
            return getRedirectPath();
        }
        return executeTimeFilterAction(flash, "Time filter preset '" + preset.getLabelEn().trim() + "' created successfully.",
                () -> timeFilterService.createPreset(preset));
    }

    @Override
    @PostMapping({"/admin/time-filters/{presetId:\\d+}/toggle", "/admin/timelines/{presetId:\\d+}/toggle"})
    public Object togglePresetSsr(@PathVariable("presetId") Long presetId,
                                  HttpServletRequest request,
                                  RedirectAttributes flash) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (!portalAccess.canManagePresets()) {
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Access denied: You do not have permission to manage time filters."));
            }
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to manage time filters.");
            return resolveRedirect(request);
        }

        try {
            TimeFilterPreset updated = timeFilterService.toggleStatus(presetId);
            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "presetId", presetId,
                        "active", updated.isActive(),
                        "message", updated.isActive() ? "Preset activated successfully." : "Preset deactivated."
                ));
            }
            flash.addFlashAttribute("successMessage", updated.isActive() ? "Preset activated successfully." : "Preset deactivated.");
        } catch (Exception ex) {
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", ex.getMessage()));
            }
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return resolveRedirect(request);
    }

    @Override
    @PostMapping({"/admin/time-filters/{presetId:\\d+}/delete", "/admin/timelines/{presetId:\\d+}/delete"})
    public Object deletePresetSsr(@PathVariable("presetId") Long presetId,
                                  HttpServletRequest request,
                                  RedirectAttributes flash) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (!portalAccess.canManagePresets()) {
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Access denied: You do not have permission to delete time filters."));
            }
            flash.addFlashAttribute("errorMessage", "Access denied: You do not have permission to delete time filters.");
            return resolveRedirect(request);
        }

        try {
            timeFilterService.deletePreset(presetId);
            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "presetId", presetId,
                        "message", "Time filter preset deleted successfully."
                ));
            }
            flash.addFlashAttribute("successMessage", "Time filter preset deleted successfully.");
        } catch (Exception ex) {
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", ex.getMessage()));
            }
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return resolveRedirect(request);
    }

    // =========================================================
    // REUSABLE HELPER ENGINE
    // =========================================================

    private String resolveRedirect(HttpServletRequest request) {
        if (request != null && request.getRequestURI() != null && request.getRequestURI().contains("/admin/timelines")) {
            return "redirect:/admin/timelines";
        }
        return "redirect:/admin/time-filters";
    }

    private String getRedirectPath() {
        var reqAttrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (reqAttrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
            return resolveRedirect(sra.getRequest());
        }
        return "redirect:/admin/time-filters";
    }

    @FunctionalInterface
    private interface ActionCallback {
        void execute() throws Exception;
    }

    /**
     * Reusable logic block that manages try-catch boundaries and routing dynamically.
     * Consolidates error feedback pipelines into one shared engine.
     */
    private String executeTimeFilterAction(RedirectAttributes flash, String successMessage, ActionCallback action) {
        try {
            action.execute();
            flash.addFlashAttribute("successMessage", successMessage);
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return getRedirectPath();
    }
}
