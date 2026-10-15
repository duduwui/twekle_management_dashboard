package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.SatisfactionReportDto;

import java.util.List;
import java.util.Map;

/**
 * Service contract for analytics and satisfaction reporting.
 * Summarizes available performance, milestones, products, and satisfaction report operations.
 */
public interface ReportService {

    Map<String, Object> generateReport(String period);

    List<SatisfactionReportDto> getSatisfactionDetails(String period);
}
