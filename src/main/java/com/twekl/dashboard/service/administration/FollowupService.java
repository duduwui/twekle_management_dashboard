package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.dto.administration.FollowupCheckRequest;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

/**
 * ============================================================================
 * Follow-up Service Contract (FollowupService.java)
 * ============================================================================
 * 
 * Defines all operations for post-sales customer follow-up management,
 * milestone checkpoints, overdue status tracking, and fulfillment progress.
 * 
 * Core Capabilities:
 *   1. Milestone Scheduling: Automatically associates active timeline presets with customer orders.
 *   2. Status Synchronization: Continuously flags overdue checks and transitions pending milestones.
 *   3. Follow-up Directory: Provides fast paged and filtered summaries of customer checkpoints.
 *   4. Checkpoint Actions: Allows staff to toggle completion, record notes, photos, and satisfaction.
 * ============================================================================
 */
public interface FollowupService {

    /**
     * Scheduled background job running every 30 seconds to synchronize overdue checks
     * against real-time UTC deadlines.
     */
    void scheduledSyncOverdueChecks();

    /**
     * Executes bulk transactional SQL updates to transition checks to OVERDUE or PENDING,
     * evicting cached summaries when updates occur.
     */
    void syncOverdueStatuses();

    /**
     * Queries a paginated list of customer follow-up summaries filtered by time cutoff,
     * status (ALERT/OVERDUE, IDLE/PENDING, DONE/COMPLETED), and customer search query.
     *
     * @param timeFilter   Preset time filter key (e.g., '24_HOURS', '7_DAYS', 'ALL').
     * @param statusFilter Follow-up status category filter.
     * @param search       Customer name, phone, city, or email search term.
     * @param pageable     Pagination and sorting parameters.
     * @return Paginated customer follow-up summary view.
     */
    Page<CustomerFollowupSummary> findPagedSummaries(
            String timeFilter,
            String statusFilter,
            String search,
            Pageable pageable);

    /**
     * Queries a paginated list of customer follow-up summaries filtered by custom date range,
     * status, and customer search query.
     *
     * @param timeFilter   Preset time filter key.
     * @param statusFilter Follow-up status category filter.
     * @param search       Customer search term.
     * @param dateFrom     Start date boundary.
     * @param dateTo       End date boundary.
     * @param pageable     Pagination and sorting parameters.
     * @return Paginated customer follow-up summary view.
     */
    Page<CustomerFollowupSummary> findPagedSummaries(
            String timeFilter,
            String statusFilter,
            String search,
            Instant dateFrom,
            Instant dateTo,
            Pageable pageable);

    /**
     * Retrieves up to 200 customer follow-up summaries for quick UI dashboard feeds
     * and cached exports.
     *
     * @param timeFilter Time filter key (e.g. '24_HOURS', '7_DAYS', 'ALL').
     * @return List of aggregated customer follow-up summaries.
     */
    List<CustomerFollowupSummary> findAllSummaries(String timeFilter);

    /**
     * Retrieves or auto-initializes all follow-up checkpoint milestones for a specific order
     * based on currently active time filter presets.
     *
     * @param orderId ID of the target customer order.
     * @return List of follow-up checks sorted chronologically by due time.
     */
    List<OrderFollowupCheck> findChecksByOrderId(Long orderId);

    /**
     * Toggles the completion status of a follow-up checkpoint (PENDING <-> COMPLETED).
     *
     * @param checkId ID of the check entity to toggle.
     * @return Updated OrderFollowupCheck entity.
     */
    OrderFollowupCheck toggleFollowupStatus(Long checkId);

    /**
     * Updates notes, proof image URL, and customer satisfaction rating on a checkpoint.
     *
     * @param checkId      ID of the check entity.
     * @param notes        Staff comments or follow-up notes.
     * @param photoUrl     Uploaded image attachment URL.
     * @param satisfaction Customer sentiment (SATISFIED, NEUTRAL, UNSATISFIED).
     * @return Updated OrderFollowupCheck entity.
     */
    OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl, String satisfaction);

    /**
     * Updates follow-up check review details including notes, photos, satisfaction, and dynamic status (PENDING, FINISHED, UNFINISHED).
     */
    OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl, String satisfaction, String status);

    /**
     * Updates follow-up check status directly to PENDING, COMPLETED (Finished), or UNFINISHED (Skipped).
     */
    OrderFollowupCheck updateCheckStatus(Long checkId, String status);

    /**
     * Marks all pending checkpoints for an order as UNFINISHED (Skipped) when supervisor does not need to review this order.
     */
    void markOrderChecksUnfinished(Long orderId);

    /**
     * Reactivates / unskips all expired or unfinished checkpoints for an order back to active status (PENDING or OVERDUE).
     */
    void unskipOrderChecks(Long orderId);

    /**
     * Marks a follow-up checkpoint as completed and saves full inspection details.
     *
     * @param checkId ID of the check entity.
     * @param req     Payload containing notes, photo, staff identity, and satisfaction.
     * @return Completed OrderFollowupCheck entity.
     */
    OrderFollowupCheck completeCheck(Long checkId, FollowupCheckRequest req);
}
