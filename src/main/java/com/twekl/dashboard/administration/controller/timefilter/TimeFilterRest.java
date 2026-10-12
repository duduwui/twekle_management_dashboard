package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterCreateService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterDeleteService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterQueryService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterStatusService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterUpdateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Dynamic Time Filter & Milestone Presets.
 * Canonical route /admin/time-filters without legacy aliases.
 */
@RestController
@RequestMapping("/admin/time-filters")
public class TimeFilterRest {

    private final TimeFilterQueryService timeFilterQueryService;
    private final TimeFilterCreateService timeFilterCreateService;
    private final TimeFilterStatusService timeFilterStatusService;
    private final TimeFilterUpdateService timeFilterUpdateService;
    private final TimeFilterDeleteService timeFilterDeleteService;

    public TimeFilterRest(TimeFilterQueryService timeFilterQueryService,
                          TimeFilterCreateService timeFilterCreateService,
                          TimeFilterStatusService timeFilterStatusService,
                          TimeFilterUpdateService timeFilterUpdateService,
                          TimeFilterDeleteService timeFilterDeleteService) {
        this.timeFilterQueryService = timeFilterQueryService;
        this.timeFilterCreateService = timeFilterCreateService;
        this.timeFilterStatusService = timeFilterStatusService;
        this.timeFilterUpdateService = timeFilterUpdateService;
        this.timeFilterDeleteService = timeFilterDeleteService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TimeFilterPreset>>> getAllPresets() {
        List<TimeFilterPreset> presets = timeFilterQueryService.findAll();
        return ResponseEntity.ok(ApiResponse.success(presets));
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TimeFilterPreset>>> getActivePresets() {
        List<TimeFilterPreset> activePresets = timeFilterQueryService.findActive();
        return ResponseEntity.ok(ApiResponse.success(activePresets));
    }

    @GetMapping(value = "/{presetId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> getPresetById(@PathVariable("presetId") Long presetId) {
        return timeFilterQueryService.findById(presetId)
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
        TimeFilterPreset created = timeFilterCreateService.createPreset(preset);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Preset created successfully", created));
    }

    @PatchMapping(value = "/{presetId:\\d+}/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> toggleActive(@PathVariable("presetId") Long presetId) {
        TimeFilterPreset toggled = timeFilterStatusService.toggleActive(presetId);
        String msg = "Preset '" + toggled.getLabelEn() + "' status updated successfully.";
        return ResponseEntity.ok(ApiResponse.success(msg, toggled));
    }

    @PutMapping(value = "/{presetId:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> updatePreset(@PathVariable("presetId") Long presetId,
                                                                      @RequestBody TimeFilterPreset updated) {
        if (updated.getLabelEn() == null && updated.getName() != null) {
            updated.setLabelEn(updated.getName());
        }
        TimeFilterPreset result = timeFilterUpdateService.updatePreset(presetId, updated);
        return ResponseEntity.ok(ApiResponse.success("Preset updated successfully", result));
    }

    @DeleteMapping(value = "/{presetId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePreset(@PathVariable("presetId") Long presetId) {
        timeFilterDeleteService.deletePreset(presetId);
        return ResponseEntity.ok(ApiResponse.success("Preset deleted successfully", null));
    }
}
