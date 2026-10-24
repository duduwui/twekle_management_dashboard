package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.FollowupCheckRequest;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Follow-up Controller Operations Contract (FollowupOperations.java)
 * ============================================================================
 * 
 * Defines all Web SSR & REST API endpoints for customer follow-up management,
 * milestone checkpoints, overdue status tracking, and inspection logs.
 * 
 * Canonical Route: /admin/followups
 * 
 * Core Capabilities:
 *   1. Follow-up Directory: SSR view of customer follow-up statuses (ALERT, IDLE, DONE).
 *   2. Paged Search: Paginated and date-filtered customer milestone checkpoints.
 *   3. Checkpoint Status: Toggles completion or records inspection notes/photos.
 *   4. Order Log: Retrieves all chronological milestone checks for a specific customer order.
 * ============================================================================
 */
public interface FollowupOperations {

    /**
     * Renders the customer follow-up directory SSR HTML page.
     *
     * @param customerId Optional customer ID to pre-focus.
     * @param model      Spring MVC Model container.
     * @return Thymeleaf view path: "administration/followup/followups".
     */
    default String showFollowups(Long customerId, Model model) {
        return showFollowups(customerId, null, null, null, null, model);
    }

    /**
     * Renders the customer follow-up directory SSR HTML page with optional pre-filters.
     *
     * @param customerId Optional customer ID to pre-focus.
     * @param timeFilter Optional time filter preset name or key.
     * @param preset     Optional alternative preset parameter.
     * @param presetId   Optional preset database ID.
     * @param status     Optional status filter.
     * @param model      Spring MVC Model container.
     * @return Thymeleaf view path: "administration/followup/followups".
     */
    String showFollowups(Long customerId, String timeFilter, String preset, Long presetId, String status, Model model);

    /**
     * Returns a paginated JSON response of customer follow-up summaries.
     *
     * @param page       Page index (1-based).
     * @param size       Page size.
     * @param timeFilter Preset time filter parameter.
     * @param status     Follow-up status filter (ALERT, IDLE, DONE).
     * @param search     Search query string.
     * @param startDate  ISO start date filter.
     * @param endDate    ISO end date filter.
     * @param sortField  Column sort property.
     * @param sortDir    Sort direction ('asc' or 'desc').
     * @return Paginated customer summaries.
     */
    ResponseEntity<ApiResponse<Map<String, Object>>> getPagedFollowups(
            int page,
            int size,
            String timeFilter,
            String status,
            String search,
            String startDate,
            String endDate,
            String sortField,
            String sortDir);

    /**
     * Toggles the completion status of a follow-up checkpoint (PENDING <-> COMPLETED).
     *
     * @param checkId Checkpoint entity ID.
     * @return Updated OrderFollowupCheck entity.
     */
    ResponseEntity<ApiResponse<OrderFollowupCheck>> toggleFollowupStatus(Long checkId);

    /**
     * Updates notes, photo URL, and satisfaction rating on a checkpoint.
     *
     * @param checkId Checkpoint entity ID.
     * @param body    Map payload with 'notes', 'photoUrl', and 'satisfaction'.
     * @return Updated OrderFollowupCheck entity.
     */
    ResponseEntity<ApiResponse<OrderFollowupCheck>> updateFollowupDetails(Long checkId, Map<String, String> body);

    /**
     * Completes a follow-up checkpoint with full inspection details.
     *
     * @param checkId Checkpoint entity ID.
     * @param request FollowupCheckRequest payload.
     * @return Completed OrderFollowupCheck entity.
     */
    ResponseEntity<ApiResponse<OrderFollowupCheck>> completeCheck(Long checkId, FollowupCheckRequest request);

    /**
     * Retrieves all milestone checks associated with a specific customer order.
     *
     * @param orderId Target customer order ID.
     * @return List of OrderFollowupCheck entities.
     */
    ResponseEntity<ApiResponse<List<OrderFollowupCheck>>> getOrderChecks(Long orderId);
}
