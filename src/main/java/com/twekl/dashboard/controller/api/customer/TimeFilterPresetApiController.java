package com.twekl.dashboard.controller.api.customer;

import com.twekl.dashboard.model.TimeFilterPreset;
import com.twekl.dashboard.service.TimeFilterPresetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Time Filters", description = "Dynamic time filter presets with duration units for customer timeline categorization")
@RestController
@RequestMapping("/api/time-filters")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class TimeFilterPresetApiController {

    private final TimeFilterPresetService service;

    @Autowired
    public TimeFilterPresetApiController(TimeFilterPresetService service) {
        this.service = service;
    }

    @Operation(summary = "List all time filter presets", description = "Retrieves all created time filter presets chronologically")
    @GetMapping
    public ResponseEntity<List<TimeFilterPreset>> getAllPresets() {
        return ResponseEntity.ok(service.getAllPresets());
    }

    @Operation(summary = "List active presets", description = "Retrieves only active presets for navigation dropdowns")
    @GetMapping("/active")
    public ResponseEntity<List<TimeFilterPreset>> getActivePresets() {
        return ResponseEntity.ok(service.getActivePresets());
    }

    @Operation(summary = "Create time filter preset", description = "Defines a custom duration filter (e.g. 14 Days, 3 Months)")
    @PostMapping
    public ResponseEntity<TimeFilterPreset> createPreset(@RequestBody TimeFilterPreset preset) {
        TimeFilterPreset created = service.createPreset(preset);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Toggle preset active state", description = "Enables or disables preset from active customer filters")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TimeFilterPreset> togglePreset(@PathVariable Long id) {
        TimeFilterPreset toggled = service.togglePresetStatus(id);
        return ResponseEntity.ok(toggled);
    }

    @Operation(summary = "Delete filter preset", description = "Removes a preset filter by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePreset(@PathVariable Long id) {
        service.deletePreset(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Filter preset deleted successfully",
                "id", id
        ));
    }
}
