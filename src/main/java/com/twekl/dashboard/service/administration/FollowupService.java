package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.dto.administration.FollowupCheckRequest;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service contract for customer follow-ups and milestone checklists.
 * Summarizes available follow-up queue evaluations, check inspections, and status transitions.
 */
public interface FollowupService {

    List<CustomerFollowupSummary> findAllSummaries(String timeFilter);

    List<OrderFollowupCheck> findChecksByOrderId(Long orderId);

    OrderFollowupCheck toggleFollowupStatus(Long checkId);

    OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl, String satisfaction);

    OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl);

    OrderFollowupCheck completeCheck(Long checkId, FollowupCheckRequest req);

    static Instant calculateDueTime(Instant start, String unit, int val) {
        if (start == null) return null;
        if (unit == null) return start.plus(val, ChronoUnit.DAYS);
        return switch (unit.toUpperCase()) {
            case "HOURS" -> start.plus(val, ChronoUnit.HOURS);
            case "DAYS" -> start.plus(val, ChronoUnit.DAYS);
            case "WEEKS" -> start.plus(val * 7L, ChronoUnit.DAYS);
            case "MONTHS" -> start.plus(val * 30L, ChronoUnit.DAYS);
            default -> start.plus(val, ChronoUnit.DAYS);
        };
    }
}
