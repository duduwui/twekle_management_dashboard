package com.twekl.dashboard.controller;

import com.twekl.dashboard.model.TimeFilterPreset;
import com.twekl.dashboard.service.TimeFilterPresetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/time-filters")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class TimeFilterPresetApiController {

    private final TimeFilterPresetService service;

    @Autowired
    public TimeFilterPresetApiController(TimeFilterPresetService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TimeFilterPreset>> getAllPresets() {
        return ResponseEntity.ok(service.getAllPresets());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TimeFilterPreset>> getActivePresets() {
        return ResponseEntity.ok(service.getActivePresets());
    }

    @PostMapping
    public ResponseEntity<TimeFilterPreset> createPreset(@RequestBody TimeFilterPreset preset) {
        TimeFilterPreset created = service.createPreset(preset);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TimeFilterPreset> togglePreset(@PathVariable Long id) {
        TimeFilterPreset toggled = service.togglePresetStatus(id);
        return ResponseEntity.ok(toggled);
    }

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
