package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.dto.administration.FollowupCheckRequest;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service contract for customer follow-up milestones and order checks.
 */
public interface FollowupService {

    List<CustomerFollowupSummary> findAllSummaries(String timeFilter);

    List<OrderFollowupCheck> findChecksByOrderId(Long orderId);

    OrderFollowupCheck toggleFollowupStatus(Long checkId);

    OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl, String satisfaction);

    OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl);

    OrderFollowupCheck completeCheck(Long checkId, FollowupCheckRequest req);

    static Instant calculateDueTime(Instant start, String unit, int val) {
        if (start == null) return Instant.now();
        String u = unit != null ? unit.toUpperCase() : "DAYS";
        int v = val > 0 ? val : 1;
        return switch (u) {
            case "HOURS" -> start.plus(v, ChronoUnit.HOURS);
            case "WEEKS" -> start.plus(v * 7L, ChronoUnit.DAYS);
            case "MONTHS" -> start.plus(v * 30L, ChronoUnit.DAYS);
            default -> start.plus(v, ChronoUnit.DAYS);
        };
    }
}
