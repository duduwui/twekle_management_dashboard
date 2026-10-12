package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterPresetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Dynamic Time Filter & Milestone Presets.
 * Serves Angular services (/internal/time-filters, /api/time-filters, /admin/api/timelines).
 */
@RestController
@RequestMapping({"/internal/time-filters", "/api/time-filters", "/admin/api/timelines"})
public class TimeFilterRestController {

    private final TimeFilterPresetService presetService;

    public TimeFilterRestController(TimeFilterPresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> getAllPresets(HttpServletRequest request) {
        List<TimeFilterPreset> presets = presetService.findAll();
        if (request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/internal/")) {
            return ResponseEntity.ok(presets);
        }
        return ResponseEntity.ok(ApiResponse.success(presets));
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> getActivePresets(HttpServletRequest request) {
        List<TimeFilterPreset> activePresets = presetService.findActive();
        if (request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/internal/")) {
            return ResponseEntity.ok(activePresets);
        }
        return ResponseEntity.ok(ApiResponse.success(activePresets));
    }

    @GetMapping(value = "/{presetId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> getPresetById(@PathVariable("presetId") Long presetId) {
        return presetService.findById(presetId)
                .map(preset -> ResponseEntity.ok(ApiResponse.success(preset)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Preset not found with ID: " + presetId)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> createPreset(@RequestBody TimeFilterPreset preset) {
        if (preset.getLabelEn() == null && preset.getName() != null) {
            preset.setLabelEn(preset.getName());
        }
        TimeFilterPreset created = presetService.createPreset(preset);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Preset created successfully", created));
    }

    @PatchMapping(value = "/{presetId:\\d+}/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> toggleActive(@PathVariable("presetId") Long presetId) {
        TimeFilterPreset toggled = presetService.toggleActive(presetId);
        String msg = "Preset '" + toggled.getLabelEn() + "' is now " + (toggled.isActive() ? "active" : "inactive");
        return ResponseEntity.ok(ApiResponse.success(msg, toggled));
    }

    @PutMapping(value = "/{presetId:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> updatePreset(@PathVariable("presetId") Long presetId,
                                                                      @RequestBody TimeFilterPreset updated) {
        if (updated.getLabelEn() == null && updated.getName() != null) {
            updated.setLabelEn(updated.getName());
        }
        TimeFilterPreset result = presetService.updatePreset(presetId, updated);
        return ResponseEntity.ok(ApiResponse.success("Preset updated successfully", result));
    }

    @DeleteMapping(value = "/{presetId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePreset(@PathVariable("presetId") Long presetId) {
        presetService.deletePreset(presetId);
        return ResponseEntity.ok(ApiResponse.success("Preset deleted successfully", null));
    }
}
