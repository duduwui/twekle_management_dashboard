package com.twekl.dashboard.controller.administrative;

import com.twekl.dashboard.dto.followup.FollowupCheckRequest;
import com.twekl.dashboard.model.order.OrderFollowupCheck;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.service.administrative.AdminService;
import com.twekl.dashboard.service.administrative.FollowupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Managing Follow-up Milestone Checks.
 * Dedicated followup domain controller with @Autowired.
 */
@RestController
@RequestMapping("/admin/followups")
public class FollowupController {

    private final FollowupService followupService;
    private final AdminService adminService;

    @Autowired
    public FollowupController(FollowupService followupService, AdminService adminService) {
        this.followupService = followupService;
        this.adminService = adminService;
    }

    private boolean canUpdate() {
        return adminService.getCurrentAdmin()
                .map(a -> a.isSuperAdmin() || a.isCanUpdateFollowups())
                .orElse(false);
    }

    @PatchMapping("/{checkId:\\d+}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> toggleFollowupStatus(@PathVariable("checkId") Long checkId) {
        if (!canUpdate()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to update follow-ups"));
        }
        OrderFollowupCheck check = followupService.toggleFollowupStatus(checkId);
        return ResponseEntity.ok(ApiResponse.success("Follow-up status updated successfully", check));
    }

    @RequestMapping(value = "/{checkId:\\d+}", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> updateFollowupDetails(
            @PathVariable("checkId") Long checkId,
            @RequestBody(required = false) Map<String, String> body) {
        if (!canUpdate()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to update follow-ups"));
        }
        String notes = body != null ? body.get("notes") : null;
        String photoUrl = body != null ? body.get("photoUrl") : null;
        String satisfaction = body != null ? body.get("satisfaction") : null;
        OrderFollowupCheck updated = followupService.updateFollowupDetails(checkId, notes, photoUrl, satisfaction);
        return ResponseEntity.ok(ApiResponse.success("Follow-up updated successfully", updated));
    }

    @PostMapping("/{checkId:\\d+}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> completeCheck(
            @PathVariable("checkId") Long checkId,
            @RequestBody(required = false) FollowupCheckRequest request) {
        if (!canUpdate()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to complete follow-ups"));
        }
        if (request == null) request = new FollowupCheckRequest();
        OrderFollowupCheck check = followupService.completeCheck(checkId, request);
        return ResponseEntity.ok(ApiResponse.success("Follow-up completed successfully", check));
    }

    @GetMapping(value = {"/orders/{orderId:\\d+}", "/orders/{orderId:\\d+}/checks"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderFollowupCheck>>> getOrderChecks(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(followupService.findChecksByOrderId(orderId)));
    }
}
