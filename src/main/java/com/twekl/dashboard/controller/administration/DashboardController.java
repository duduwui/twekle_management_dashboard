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
        List<CustomerFollowupSummary> summaries = followupService.findAllSummaries("ALL");
        List<CustomerFollowupSummary> urgent = summaries.stream()
                .filter(s -> !s.isAllFollowupsCompleted() && ("ALERT".equalsIgnoreCase(s.getFollowupStatus()) || "IDLE".equalsIgnoreCase(s.getFollowupStatus())))
                .sorted(Comparator.comparing(CustomerFollowupSummary::getNextPendingMinutesLeft,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("stats", customerService.getStats());
        model.addAttribute("urgentCustomers", urgent);
        model.addAttribute("allCustomers", summaries);
        model.addAttribute("satisfactionDetails", reportService.getSatisfactionDetails("ALL").stream()
                .filter(item -> item.getRating() <= 2 || "UNSATISFIED".equalsIgnoreCase(item.getSatisfaction()))
                .toList());
        return "administration/dashboard/dashboard";
    }

    @GetMapping(value = "/admin/stats/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
}

