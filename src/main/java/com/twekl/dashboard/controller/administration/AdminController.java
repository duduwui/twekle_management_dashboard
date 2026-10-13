package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.service.administration.*;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.util.Comparator;
import java.util.List;

/**
 * Primary Administrative & Web Portal Controller.
 * Consolidates SSR view rendering and administrative actions.
 */
@Controller
public class AdminController {

    private final AdminService adminService;
    private final RoleService roleService;
    private final CustomerService customerService;
    private final FollowupService followupService;
    private final ReportService reportService;
    private final TimeFilterService timeFilterService;

    public AdminController(AdminService adminService,
                           RoleService roleService,
                           CustomerService customerService,
                           FollowupService followupService,
                           ReportService reportService,
                           TimeFilterService timeFilterService) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.customerService = customerService;
        this.followupService = followupService;
        this.reportService = reportService;
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

    // =========================================================
    // GENERAL PORTAL & AUTH ENDPOINTS
    // =========================================================

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

    // =========================================================
    // STAFF ADMINISTRATORS ENDPOINTS
    // =========================================================

    @GetMapping("/admin/admins")
    public String showAdmins(Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "admins");
        model.addAttribute("admins", adminService.findAll());
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newAdmin")) {
            model.addAttribute("newAdmin", new CreateAdminRequest());
        }
        return "administration/admin/admins";
    }

    @PostMapping("/admin/admins/create-admin")
    public String createAdminSsr(@Valid @ModelAttribute("newAdmin") CreateAdminRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes flash) {
        if (bindingResult.hasErrors()) {
            flash.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/admins";
        }
        try {
            adminService.createAdmin(request);
            flash.addFlashAttribute("successMessage", "Administrator '" + request.getUsername() + "' created successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping("/admin/admins/{adminId:\\d+}/toggle-status")
    public String toggleAdminStatusSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            Admin toggled = adminService.toggleStatus(adminId);
            flash.addFlashAttribute("successMessage", "Administrator '" + toggled.getUsername() + "' status updated successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping("/admin/admins/{adminId:\\d+}/delete-admin")
    public String deleteAdminSsr(@PathVariable("adminId") Long adminId, RedirectAttributes flash) {
        try {
            adminService.deleteAdmin(adminId);
            flash.addFlashAttribute("successMessage", "Administrator deleted successfully.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
