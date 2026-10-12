package com.twekl.dashboard.administration.controller.web;

import com.twekl.dashboard.administration.dto.customer.CustomerFollowupSummary;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.customer.CustomerService;
import com.twekl.dashboard.administration.service.report.ReportService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterPresetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Primary Web Controller for the Admin Portal.
 * Handles server-rendered Thymeleaf views with pre-populated Models.
 */
@Controller
public class AdminWebController {

    private final CustomerService customerService;
    private final ReportService reportService;
    private final AdminService adminService;
    private final TimeFilterPresetService timeFilterPresetService;

    public AdminWebController(CustomerService customerService,
                              ReportService reportService,
                              AdminService adminService,
                              TimeFilterPresetService timeFilterPresetService) {
        this.customerService = customerService;
        this.reportService = reportService;
        this.adminService = adminService;
        this.timeFilterPresetService = timeFilterPresetService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    @ModelAttribute("currentUser")
    public Admin populateCurrentUser() {
        return adminService.getCurrentAdmin().orElse(null);
    }

    @GetMapping("/login")
    public String showLoginForm() {
        if (isAuthenticated()) {
            return "redirect:/admin/dashboard";
        }
        return "administration/login";
    }

    @PostMapping({"/admin/logout", "/logout"})
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return "redirect:/login";
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

        List<CustomerFollowupSummary> summaries = customerService.findAllSummaries("ALL");
        List<CustomerFollowupSummary> urgent = summaries.stream()
                .filter(s -> !s.isAllFollowupsCompleted() && ("ALERT".equalsIgnoreCase(s.getFollowupStatus()) || "IDLE".equalsIgnoreCase(s.getFollowupStatus())))
                .toList();

        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("stats", customerService.getStats());
        model.addAttribute("urgentCustomers", urgent);
        model.addAttribute("allCustomers", summaries);
        model.addAttribute("satisfactionDetails", reportService.getSatisfactionDetails("ALL"));
        return "administration/dashboard/dashboard";
    }

    @GetMapping(value = {"/admin/followups", "/admin/followups/**", "/admin/customers", "/admin/customers/**", "/followups", "/followups/**", "/customers", "/customers/**"}, produces = MediaType.TEXT_HTML_VALUE)
    public String showCustomers(Model model) {
        if (!isAuthenticated()) return "redirect:/login";

        model.addAttribute("activeTab", "followups");
        model.addAttribute("followupSummaries", customerService.findAllSummaries("ALL"));
        model.addAttribute("presets", timeFilterPresetService.findAll());
        return "administration/customer/customers";
    }

    @GetMapping(value = {"/admin/reports", "/reports"}, produces = MediaType.TEXT_HTML_VALUE)
    public String showReports(@RequestParam(value = "period", defaultValue = "ALL") String period,
                              @RequestParam(value = "tab", defaultValue = "milestones") String tab,
                              Model model) {
        if (!isAuthenticated()) return "redirect:/login";

        // Permission check: redirect non-authorized users to dashboard
        Admin admin = adminService.getCurrentAdmin().orElse(null);
        if (admin != null && !admin.isSuperAdmin() && !admin.isCanReadReports()) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("activeTab", "reports");
        model.addAttribute("reportSubTab", tab);
        model.addAttribute("period", period);
        model.addAttribute("reportData", reportService.generateReport(period));
        model.addAttribute("presets", timeFilterPresetService.findAll());
        return "administration/report/reports";
    }
}

