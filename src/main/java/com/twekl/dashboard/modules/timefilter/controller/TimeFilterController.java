package com.twekl.dashboard.modules.timefilter.controller;

import com.twekl.dashboard.common.dto.ApiResponse;
import com.twekl.dashboard.modules.timefilter.dto.TimeFilterPresetDto;
import com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset;
import com.twekl.dashboard.modules.timefilter.service.TimeFilterPresetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/internal/time-filters", "/api/time-filters"})
public class TimeFilterController {

    private final TimeFilterPresetService presetService;

    public TimeFilterController(TimeFilterPresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping
    public ResponseEntity<List<TimeFilterPreset>> getAllPresets(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(presetService.findActive());
        }
        return ResponseEntity.ok(presetService.findAll());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TimeFilterPreset>> createPreset(@Valid @RequestBody TimeFilterPresetDto dto) {
        TimeFilterPreset created = presetService.createPreset(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Time filter preset created successfully", created));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<TimeFilterPreset>> togglePreset(@PathVariable Long id) {
        TimeFilterPreset updated = presetService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success("Time filter preset toggled successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePreset(@PathVariable Long id) {
        presetService.deletePreset(id);
        return ResponseEntity.ok(ApiResponse.success("Time filter preset deleted successfully", null));
    }
}
