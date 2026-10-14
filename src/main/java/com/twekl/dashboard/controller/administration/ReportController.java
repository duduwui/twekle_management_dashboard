package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.SatisfactionReportDto;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.response.administration.ApiResponse;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.ReportService;
import com.twekl.dashboard.service.administration.TimeFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Controller for Analytics and Satisfaction Reports.
 * Dedicated report domain controller.
 */
@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    private final ReportService reportService;
    private final AdminService adminService;
    private final TimeFilterService timeFilterService;

    @Autowired
    public ReportController(final ReportService reportService,
                            final AdminService adminService,
                            final TimeFilterService timeFilterService) {
        this.reportService = reportService;
        this.adminService = adminService;
        this.timeFilterService = timeFilterService;
    }

    @GetMapping
    public String showReports(@RequestParam(value = "period", defaultValue = "ALL") String period,
                              @RequestParam(value = "tab", defaultValue = "milestones") String tab,
                              Model model) {
        Admin admin = adminService.getCurrentAdmin().orElse(null);
        if (admin != null && !admin.isSuperAdmin() && !admin.isCanReadReports()) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("activeTab", "reports");
        model.addAttribute("reportSubTab", tab);
        model.addAttribute("period", period);
        model.addAttribute("reportData", reportService.generateReport(period));
        model.addAttribute("presets", timeFilterService.findAll());
        return "administration/report/reports";
    }

    @GetMapping(value = {"", "/data"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateAnalyticsReport(
            @RequestParam(required = false, defaultValue = "ALL") String period) {
        Map<String, Object> data = reportService.generateReport(period);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping(value = "/satisfaction", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<List<SatisfactionReportDto>>> getSatisfactionDetails(
            @RequestParam(required = false, defaultValue = "ALL") String period) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSatisfactionDetails(period)));
    }

    @GetMapping("/export")
    @ResponseBody
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
              .append("\"").append(item.getFeedbackDate() != null ? item.getFeedbackDate() : "").append("\",")
              .append("\"").append(item.getComment() != null ? item.getComment().replace("\"", "\"\"") : "").append("\"\n");
        }

        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"satisfaction-report-" + period + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }
}

