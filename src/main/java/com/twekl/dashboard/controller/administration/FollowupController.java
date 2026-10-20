package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.FollowupCheckRequest;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.service.administration.AdminService;
import com.twekl.dashboard.service.administration.FollowupService;
import com.twekl.dashboard.service.administration.TimeFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Managing Follow-up Milestone Checks and Followups View.
 * Dedicated followup domain controller.
 */
@Controller
@RequestMapping("/admin/followups")
public class FollowupController {

    private final FollowupService followupService;
    private final AdminService adminService;
    private final TimeFilterService timeFilterService;

    @Autowired
    public FollowupController(final FollowupService followupService,
                              final AdminService adminService,
                              final TimeFilterService timeFilterService) {
        this.followupService = followupService;
        this.adminService = adminService;
        this.timeFilterService = timeFilterService;
    }

    private boolean hasAction(String actionKey) {
        return adminService.getCurrentAdmin()
                .map(a -> a.hasAction(actionKey))
                .orElse(false);
    }

    private boolean canRead() {
        return hasAction("CUSTOMER_VIEW_LIST");
    }

    private boolean canToggleCheck() {
        return hasAction("CUSTOMER_CHECK_TOGGLE");
    }

    private boolean canUpdateDetails() {
        return hasAction("CUSTOMER_ADD_FEEDBACK") || hasAction("CUSTOMER_UPLOAD_PHOTOS");
    }

    @GetMapping({"", "/", "/orders/{customerId:\\d+}", "/feedback/{customerId:\\d+}"})
    public String showFollowups(@PathVariable(value = "customerId", required = false) Long customerId, Model model) {
        if (!canRead()) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("activeTab", "followups");
        org.springframework.data.domain.Page<com.twekl.dashboard.dto.administration.CustomerFollowupSummary> initialPage =
                followupService.findPagedSummaries("ALL", "ALL", null,
                        org.springframework.data.domain.PageRequest.of(0, 40, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
        model.addAttribute("followupSummaries", initialPage.getContent());
        model.addAttribute("totalCustomers", initialPage.getTotalElements());
        model.addAttribute("totalPages", initialPage.getTotalPages());
        model.addAttribute("currentPage", 1);
        model.addAttribute("pageSize", 40);
        model.addAttribute("presets", timeFilterService.findAll());
        if (customerId != null) {
            model.addAttribute("targetCustomerId", customerId);
        }
        return "administration/followup/followups";
    }

    @GetMapping(value = "/paged", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPagedFollowups(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "40") int size,
            @RequestParam(value = "timeFilter", required = false) String timeFilter,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "sortField", defaultValue = "id") String sortField,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

        if (!canRead()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to view follow-ups"));
        }

        int pageZeroIndex = Math.max(0, page - 1);
        int pageSize = Math.min(100, Math.max(10, size));
        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        String field = "id";
        if ("totalOrders".equalsIgnoreCase(sortField)) field = "totalOrders";
        else if ("customerType".equalsIgnoreCase(sortField)) field = "name";
        else if ("name".equalsIgnoreCase(sortField)) field = "name";
        else if ("phoneNumber".equalsIgnoreCase(sortField)) field = "phoneNumber";
        else if ("createdAt".equalsIgnoreCase(sortField)) field = "createdAt";

        java.time.Instant dateFrom = null;
        java.time.Instant dateTo = null;
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                dateFrom = java.time.LocalDate.parse(startDate.trim()).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                dateTo = java.time.LocalDate.parse(endDate.trim()).plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {}
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(pageZeroIndex, pageSize, org.springframework.data.domain.Sort.by(direction, field));
        org.springframework.data.domain.Page<com.twekl.dashboard.dto.administration.CustomerFollowupSummary> pagedResult =
                followupService.findPagedSummaries(timeFilter, status, search, dateFrom, dateTo, pageable);

        Map<String, Object> responseData = new java.util.LinkedHashMap<>();
        responseData.put("content", pagedResult.getContent());
        responseData.put("currentPage", page);
        responseData.put("pageSize", pageSize);
        responseData.put("totalElements", pagedResult.getTotalElements());
        responseData.put("totalPages", pagedResult.getTotalPages());
        responseData.put("first", pagedResult.isFirst());
        responseData.put("last", pagedResult.isLast());

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @PatchMapping("/{checkId:\\d+}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> toggleFollowupStatus(@PathVariable("checkId") Long checkId) {
        if (!canToggleCheck()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to toggle follow-up checkpoint status"));
        }
        OrderFollowupCheck check = followupService.toggleFollowupStatus(checkId);
        return ResponseEntity.ok(ApiResponse.success("Follow-up status updated successfully", check));
    }

    @RequestMapping(value = "/{checkId:\\d+}", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> updateFollowupDetails(
            @PathVariable("checkId") Long checkId,
            @RequestBody(required = false) Map<String, String> body) {
        if (!canUpdateDetails()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to edit follow-up notes or photos"));
        }
        String notes = body != null ? body.get("notes") : null;
        String photoUrl = body != null ? body.get("photoUrl") : null;
        String satisfaction = body != null ? body.get("satisfaction") : null;
        OrderFollowupCheck updated = followupService.updateFollowupDetails(checkId, notes, photoUrl, satisfaction);
        return ResponseEntity.ok(ApiResponse.success("Follow-up updated successfully", updated));
    }

    @PostMapping("/{checkId:\\d+}/complete")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> completeCheck(
            @PathVariable("checkId") Long checkId,
            @RequestBody(required = false) FollowupCheckRequest request) {
        if (!canToggleCheck()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to complete follow-ups"));
        }
        if (request == null) request = new FollowupCheckRequest();
        OrderFollowupCheck check = followupService.completeCheck(checkId, request);
        return ResponseEntity.ok(ApiResponse.success("Follow-up completed successfully", check));
    }

    @GetMapping(value = "/orders/{orderId:\\d+}/checks", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<List<OrderFollowupCheck>>> getOrderChecks(@PathVariable("orderId") Long orderId) {
        if (!hasAction("CUSTOMER_VIEW_LOG")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to view order follow-up logs"));
        }
        return ResponseEntity.ok(ApiResponse.success(followupService.findChecksByOrderId(orderId)));
    }
}

