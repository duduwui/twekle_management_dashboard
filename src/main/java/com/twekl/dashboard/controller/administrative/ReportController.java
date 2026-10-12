package com.twekl.dashboard.controller.administrative;

import com.twekl.dashboard.dto.administrative.SatisfactionReportDto;
import com.twekl.dashboard.response.administrative.ApiResponse;
import com.twekl.dashboard.service.administrative.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Controller for Analytics and Satisfaction Reports.
 * Dedicated report domain controller with @Autowired.
 */
@RestController
@RequestMapping("/admin/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    public ReportController() {
    }

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateAnalyticsReport(
            @RequestParam(required = false, defaultValue = "ALL") String period) {
        Map<String, Object> data = reportService.generateReport(period);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping(value = "/satisfaction", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SatisfactionReportDto>>> getSatisfactionDetails(
            @RequestParam(required = false, defaultValue = "ALL") String period) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSatisfactionDetails(period)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportSatisfactionCsv(@RequestParam(required = false, defaultValue = "ALL") String period) {
        List<SatisfactionReportDto> list = reportService.getSatisfactionDetails(period);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Customer Name,Phone,City,Order ID,Order Number,Rating,Feedback Date,Comment\n");

        for (SatisfactionReportDto item : list) {
            sb.append(item.getId() != null ? item.getId() : "").append(",")
              .append("\"").append(item.getCustomerName() != null ? item.getCustomerName().replace("\"", "\"\"") : "").append("\",")
              .append("\"").append(item.getPhoneNumber() != null ? item.getPhoneNumber() : "").append("\",")
              .append("\"").append(item.getCity() != null ? item.getCity() : "").append("\",")
              .append(item.getOrderId() != null ? item.getOrderId() : "").append(",")
              .append("\"").append(item.getOrderNumber() != null ? item.getOrderNumber() : "").append("\",")
              .append(item.getRating()).append(",")
              .append("\"").append(item.getFeedbackDate() != null ? item.getFeedbackDate().toString() : "").append("\",")
              .append("\"").append(item.getComment() != null ? item.getComment().replace("\"", "\"\"") : "").append("\"\n");
        }

        byte[] bytes = ("\uFEFF" + sb.toString()).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Twekl_Report_satisfaction_" + period.toLowerCase() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
