package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterPresetService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Timeline & Milestone Presets Directory.
 */
@Controller
@RequestMapping("/admin/timelines")
public class TimeFilterList {

    private final TimeFilterPresetService presetService;
    private final AdminService adminService;

    public TimeFilterList(TimeFilterPresetService presetService, AdminService adminService) {
        this.presetService = presetService;
        this.adminService = adminService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    @GetMapping
    public String showTimelines(Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "timelines");
        model.addAttribute("presetList", presetService.findAll());
        model.addAttribute("currentUser", adminService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newPreset")) {
            model.addAttribute("newPreset", new TimeFilterPreset());
        }
        return "administration/timefilter/timeFilters";
    }
}
