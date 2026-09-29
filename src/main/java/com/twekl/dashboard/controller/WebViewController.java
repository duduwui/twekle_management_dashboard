package com.twekl.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class WebViewController {

    @GetMapping(value = {
        "/", 
        "/login", 
        "/admin", 
        "/admin/**", 
        "/followups", 
        "/followups/**", 
        "/users", 
        "/users/**", 
        "/roles", 
        "/roles/**", 
        "/role-templates", 
        "/role-templates/**",
        "/admins", 
        "/admins/**", 
        "/customers", 
        "/customers/**"
    })
    public String index(Model model) {
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH));
        model.addAttribute("currentDate", formattedDate);
        model.addAttribute("appName", "Twekl Management Dashboard");
        return "index";
    }
}
