package com.twekl.dashboard.controller;

import com.twekl.dashboard.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class ReportApiController {

    private final ReportService reportService;

    @Autowired
    public ReportApiController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/followups")
    public ResponseEntity<Map<String, Object>> getFollowupReport(
            @RequestParam(value = "period", required = false, defaultValue = "all") String period,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo) {
        Map<String, Object> report = reportService.generateFollowupReport(period, dateFrom, dateTo);
        return ResponseEntity.ok(report);
    }
}
