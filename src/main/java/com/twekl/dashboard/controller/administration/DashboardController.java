package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.service.administration.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for Summary Statistics and KPIs.
 * Dedicated dashboard domain controller.
 */
@RestController
@RequestMapping("/admin/stats")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(final DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
}
