package com.twekl.dashboard.administration.controller.timefilter;

import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.service.admin.AdminQueryService;
import com.twekl.dashboard.administration.service.timefilter.TimeFilterQueryService;
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

    private final TimeFilterQueryService timeFilterQueryService;
    private final AdminQueryService adminQueryService;

    public TimeFilterList(TimeFilterQueryService timeFilterQueryService, AdminQueryService adminQueryService) {
        this.timeFilterQueryService = timeFilterQueryService;
        this.adminQueryService = adminQueryService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    @GetMapping
    public String showTimelines(Model model) {
        if (!isAuthenticated()) return "redirect:/login";
        model.addAttribute("activeTab", "timelines");
        model.addAttribute("presetList", timeFilterQueryService.findAll());
        model.addAttribute("currentUser", adminQueryService.getCurrentAdmin().orElse(null));
        if (!model.containsAttribute("newPreset")) {
            model.addAttribute("newPreset", new TimeFilterPreset());
        }
        return "administration/timefilter/timeFilters";
    }
}
