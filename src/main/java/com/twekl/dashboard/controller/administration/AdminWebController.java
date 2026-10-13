package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.CustomerService;
import com.twekl.dashboard.service.administration.FollowupService;
import com.twekl.dashboard.service.administration.ReportService;
import com.twekl.dashboard.service.administration.TimeFilterService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Primary Web View Controller for the Admin Portal.
 * Handles server-rendered Thymeleaf views with pre-populated Models.
 */
@Controller
public class AdminWebController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private FollowupService followupService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private TimeFilterService timeFilterService;

    public AdminWebController() {
    }

    @Autowired
    public AdminWebController(CustomerService customerService,
                              FollowupService followupService,
                              ReportService reportService,
                              AdminService adminService,
                              TimeFilterService timeFilterService) {
        this.customerService = customerService;
        this.followupService = followupService;
        this.reportService = reportService;
        this.adminService = adminService;
        this.timeFilterService = timeFilterService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    @ModelAttribute("currentUser")
    public Admin populateCurrentUser() {
        return adminService.getCurrentAdmin().orElse(null);
    }

    @GetMapping(value = "/k6-report.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<Resource> showK6Report() {
        File file = new File("src/main/webapp/k6-report.html");
        if (!file.exists()) {
            file = new File("/opt/twekl-dashboard/src/main/webapp/k6-report.html");
        }
        if (file.exists()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(new FileSystemResource(file));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/login")
    public String showLoginForm() {
        if (isAuthenticated()) {
            return "redirect:/admin/dashboard";
        }
        return "administration/login";
    }

    @GetMapping({"/", "/admin", "/admin/"})
    public String handleRootRedirect() {
        if (isAuthenticated()) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/admin/dashboard")
    public String showDashboard(Model model) {
        if (!isAuthenticated()) return "redirect:/login";

        List<CustomerFollowupSummary> summaries = followupService.findAllSummaries("ALL");
        List<CustomerFollowupSummary> urgent = summaries.stream()
                .filter(s -> !s.isAllFollowupsCompleted() && ("ALERT".equalsIgnoreCase(s.getFollowupStatus()) || "IDLE".equalsIgnoreCase(s.getFollowupStatus())))
                .sorted(java.util.Comparator.comparing(CustomerFollowupSummary::getNextPendingMinutesLeft,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
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

    @GetMapping(value = {"/admin/followups", "/admin/followups/**"}, produces = MediaType.TEXT_HTML_VALUE)
    public String showCustomers(Model model) {
        if (!isAuthenticated()) return "redirect:/login";

        model.addAttribute("activeTab", "followups");
        model.addAttribute("followupSummaries", followupService.findAllSummaries("ALL"));
        model.addAttribute("presets", timeFilterService.findAll());
        return "administration/followup/followups";
    }

    @GetMapping(value = "/admin/customers", produces = MediaType.TEXT_HTML_VALUE)
    public String redirectCustomers() {
        return "redirect:/admin/followups";
    }

    @GetMapping(value = "/admin/reports", produces = MediaType.TEXT_HTML_VALUE)
    public String showReports(@RequestParam(value = "period", defaultValue = "ALL") String period,
                              @RequestParam(value = "tab", defaultValue = "milestones") String tab,
                              Model model) {
        if (!isAuthenticated()) return "redirect:/login";

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
}
