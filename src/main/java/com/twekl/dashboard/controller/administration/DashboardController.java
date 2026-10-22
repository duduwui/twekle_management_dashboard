package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.service.administration.CustomerService;
import com.twekl.dashboard.service.administration.DashboardService;
import com.twekl.dashboard.service.administration.FollowupService;
import com.twekl.dashboard.service.administration.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Controller for Dashboard Views and Summary Statistics.
 * Dedicated dashboard domain controller.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final CustomerService customerService;
    private final FollowupService followupService;
    private final ReportService reportService;

    @Autowired
    public DashboardController(final DashboardService dashboardService,
                               final CustomerService customerService,
                               final FollowupService followupService,
                               final ReportService reportService) {
        this.dashboardService = dashboardService;
        this.customerService = customerService;
        this.followupService = followupService;
        this.reportService = reportService;
    }

    @GetMapping("/admin/dashboard")
    public String showDashboard(Model model) {
        org.springframework.data.domain.Page<CustomerFollowupSummary> recentPage = followupService.findPagedSummaries("ALL", "ALL", null,
                org.springframework.data.domain.PageRequest.of(0, 100, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
        org.springframework.data.domain.Page<CustomerFollowupSummary> urgentPage = followupService.findPagedSummaries("ALL", "ALERT", null,
                org.springframework.data.domain.PageRequest.of(0, 50, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));

        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("stats", customerService.getStats());
        model.addAttribute("urgentCustomers", urgentPage.getContent());
        model.addAttribute("allCustomers", recentPage.getContent());
        model.addAttribute("satisfactionDetails", reportService.getSatisfactionDetails("ALL").stream()
                .filter(item -> item.getRating() <= 2 || "UNSATISFIED".equalsIgnoreCase(item.getSatisfaction()))
                .limit(40)
                .toList());
        return "administration/dashboard/dashboard";
    }

    @GetMapping(value = "/admin/stats/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
}

