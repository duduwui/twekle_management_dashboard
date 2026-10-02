package com.twekl.dashboard.modules.report.controller;

import com.twekl.dashboard.modules.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/internal/reports", "/api/reports"})
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getReport(
            @RequestParam(required = false, defaultValue = "monthly") String period) {
        return ResponseEntity.ok(reportService.generateReport(period));
    }
}
