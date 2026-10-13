package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.repository.administration.PortalQueryRepository;
import com.twekl.dashboard.security.administration.PortalAccess;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWebController {
    private final PortalQueryRepository queries;
    private final PortalAccess access;

    public AdminWebController(PortalQueryRepository queries, PortalAccess access) {
        this.queries = queries;
        this.access = access;
    }

    @GetMapping("/login")
    public String login() {
        return access.getCurrent() == null ? "administration/login" : "redirect:/admin/dashboard";
    }

    @GetMapping({"/", "/admin", "/admin/"})
    public String root() {
        if (access.allows("CUSTOMER_FOLLOWUP", "READ")) return "redirect:/admin/dashboard";
        if (access.allows("REPORTS_INSIGHTS", "READ")) return "redirect:/admin/reports";
        if (access.staff()) return "redirect:/admin/admins";
        if (access.roles()) return "redirect:/admin/roles";
        if (access.allows("TIMELINES_PRESETS", "READ")) return "redirect:/admin/timelines";
        throw new org.springframework.security.access.AccessDeniedException(
                "No portal permissions have been assigned");
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("@portalAccess.allows('CUSTOMER_FOLLOWUP','READ')")
    public String dashboard(Model model) {
        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("stats", queries.stats());
        model.addAttribute("urgentCustomers", queries.priorityCustomers());
        return "administration/dashboard/dashboard";
    }
}
