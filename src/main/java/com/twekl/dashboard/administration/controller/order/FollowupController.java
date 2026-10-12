package com.twekl.dashboard.administration.controller.order;

import com.twekl.dashboard.administration.dto.order.FollowupCheckRequest;
import com.twekl.dashboard.administration.model.order.OrderFollowupCheck;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.admin.AdminService;
import com.twekl.dashboard.administration.service.customer.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller for Managing Follow-up Milestone Checks.
 * Standardized under /admin/followups route with explicit domain naming.
 */
@RestController
@RequestMapping({"/admin/followups", "/admin/api/followups", "/internal/followups", "/api/followups"})
public class FollowupController {

    private final CustomerService customerService;
    private final AdminService adminService;

    public FollowupController(CustomerService customerService, AdminService adminService) {
        this.customerService = customerService;
        this.adminService = adminService;
    }

    /**
     * Returns 403 Forbidden if the current user cannot update follow-ups.
     * Super admins always pass. For others, checks CUSTOMER_FOLLOWUP update permission.
     */
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
        OrderFollowupCheck check = customerService.toggleFollowupStatus(checkId);
        return ResponseEntity.ok(ApiResponse.success("Status updated", check));
    }

    @RequestMapping(value = "/{checkId:\\d+}", method = {org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.PUT})
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
        OrderFollowupCheck updated = customerService.updateFollowupDetails(checkId, notes, photoUrl, satisfaction);
        return ResponseEntity.ok(ApiResponse.success("Follow-up updated", updated));
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
        OrderFollowupCheck check = customerService.completeCheck(checkId, request);
        return ResponseEntity.ok(ApiResponse.success("Follow-up completed successfully", check));
    }

    @GetMapping(value = {"/orders/{orderId:\\d+}", "/orders/{orderId:\\d+}/checks"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderFollowupCheck>>> getOrderChecks(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(customerService.findChecksByOrderId(orderId)));
    }
}

