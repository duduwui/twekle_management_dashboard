package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.SatisfactionReportDto;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Analytics & Reporting Service Contract (ReportService.java)
 * ============================================================================
 * 
 * Defines all comprehensive executive reporting capabilities, customer retention
 * loyalty tiers, product defect ratios, milestone progress matrices, and sentiment logs.
 * 
 * Core Capabilities:
 *   1. Executive Periodic Reports: Generates 360-degree analytics across any time filter or custom date range.
 *   2. Retention & Loyalty Tiers: Computes distribution of Loyal (frequent repeat), Regular, and One-Time customers.
 *   3. Product Quality Health: Aggregates order volumes, defect rates, and satisfaction sentiments per catalog product.
 *   4. Sentiment Ledger: Provides chronological audit trails of customer satisfaction reviews and staff notes.
 * ============================================================================
 */
public interface ReportService {

    /**
     * Generates a complete periodic analytical report for predefined time filter presets
     * (e.g., '24_HOURS', '7_DAYS', '30_DAYS', 'ALL').
     *
     * @param period Filter preset key.
     * @return Complete structured report payload with summary, loyalty, milestones, and products.
     */
    Map<String, Object> generateReport(String period);

    /**
     * Generates a complete analytical report for a custom date range (ISO yyyy-MM-dd).
     *
     * @param period   Preset filter label or 'CUSTOM'.
     * @param dateFrom Start date boundary (e.g. '2026-10-01').
     * @param dateTo   End date boundary (e.g. '2026-10-24').
     * @return Complete structured report payload.
     */
    Map<String, Object> generateReport(String period, String dateFrom, String dateTo);

    /**
     * Retrieves detailed customer satisfaction feedback reviews and milestone check sentiments,
     * sorted chronologically for detailed staff analysis.
     *
     * @param period Filter preset key.
     * @return List of SatisfactionReportDto objects.
     */
    List<SatisfactionReportDto> getSatisfactionDetails(String period);
}
