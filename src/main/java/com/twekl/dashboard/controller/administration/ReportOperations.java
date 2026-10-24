package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.SatisfactionReportDto;
import com.twekl.dashboard.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Analytics & Report Controller Operations Contract (ReportOperations.java)
 * ============================================================================
 * 
 * Defines all Web SSR & REST API endpoints for executive reporting,
 * customer retention loyalty tiers, product defect ratios, and sentiment CSV exports.
 * 
 * Canonical Route: /admin/reports
 * 
 * Core Capabilities:
 *   1. Reports View: SSR rendering of multi-tab executive reports (milestones, satisfaction, products, retention).
 *   2. Analytics API: Structured JSON report for periodic or custom date range aggregations.
 *   3. Sentiment Ledger: Chronological list of customer feedback ratings and milestone notes.
 *   4. CSV Export: Secure CSV download of customer satisfaction reviews.
 * ============================================================================
 */
public interface ReportOperations {

    /**
     * Renders the executive analytics reports SSR HTML page.
     *
     * @param period   Filter preset parameter.
     * @param tab      Active sub-tab ('milestones', 'satisfaction', 'products', 'retention').
     * @param dateFrom Custom start date.
     * @param dateTo   Custom end date.
     * @param model    Spring MVC Model container.
     * @return Thymeleaf view path: "administration/report/reports".
     */
    String showReports(String period, String tab, String dateFrom, String dateTo, Model model);

    /**
     * Returns structured analytics report payload for a given time period or date range.
     *
     * @param period   Filter period key (default 'ALL').
     * @param dateFrom Custom start date.
     * @param dateTo   Custom end date.
     * @return Complete JSON report.
     */
    ResponseEntity<ApiResponse<Map<String, Object>>> generateAnalyticsReport(String period, String dateFrom, String dateTo);

    /**
     * Retrieves detailed customer satisfaction sentiment logs and ratings.
     *
     * @param period Filter period key.
     * @return List of SatisfactionReportDto items.
     */
    ResponseEntity<ApiResponse<List<SatisfactionReportDto>>> getSatisfactionDetails(String period);

    /**
     * Generates and downloads a CSV export of customer satisfaction data.
     *
     * @param period Filter period key.
     * @return Byte array containing CSV file contents.
     */
    ResponseEntity<byte[]> exportSatisfactionCsv(String period);
}
