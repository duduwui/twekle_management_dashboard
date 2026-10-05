package com.twekl.dashboard.modules.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    @GetMapping({
        "/",
        "/admin",
        "/admin/",
        "/admin/followups",
        "/admin/followups/**",
        "/admin/users",
        "/admin/users/**",
        "/admin/admins",
        "/admin/admins/**",
        "/admin/time-filters",
        "/admin/time-filters/**",
        "/admin/role-templates",
        "/admin/role-templates/**",
        "/admin/reports",
        "/admin/reports/**"
    })
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "index";
    }
}
