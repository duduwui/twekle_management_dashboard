package com.twekl.dashboard.controller.administration;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.util.Map;

/**
 * ============================================================================
 * Dashboard Controller Operations Contract (DashboardOperations.java)
 * ============================================================================
 * 
 * Defines all Web & REST API endpoint signatures for the Executive Dashboard,
 * live KPI aggregations, milestone progress graphs, and satisfaction sentiment charts.
 * 
 * Canonical Route: /admin/dashboard & /admin/stats/summary
 * 
 * Core Capabilities:
 *   1. Executive View: SSR rendering of main management dashboard with urgent priorities.
 *   2. Real-time Summary API: Fast cached JSON payload of all operational statistics and charts.
 * ============================================================================
 */
public interface DashboardOperations {

    /**
     * Renders the executive administration dashboard SSR HTML page.
     *
     * @param model Spring MVC Model container.
     * @return Thymeleaf view path: "administration/dashboard/dashboard".
     */
    String showDashboard(Model model);

    /**
     * Returns a JSON representation of all live system KPIs, milestone breakdowns,
     * and satisfaction distribution.
     *
     * @return ResponseEntity with complete dashboard summary map.
     */
    ResponseEntity<Map<String, Object>> getDashboardSummary();
}
