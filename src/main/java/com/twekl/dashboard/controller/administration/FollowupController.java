package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.FollowupCheckRequest;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
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
 * Implements FollowupOperations interface contract.
 */
@Controller
@RequestMapping("/admin/followups")
public class FollowupController implements FollowupOperations {

    private final FollowupService followupService;
    private final AdminService adminService;
    private final TimeFilterService timeFilterService;

    @Autowired(required = false)
    private com.twekl.dashboard.service.administration.ExternalIntegrationService externalIntegrationService;

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

    @Override
    @GetMapping({"", "/", "/orders/{customerId:\\d+}", "/feedback/{customerId:\\d+}"})
    public String showFollowups(
            @PathVariable(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "timeFilter", required = false) String timeFilter,
            @RequestParam(value = "preset", required = false) String preset,
            @RequestParam(value = "presetId", required = false) Long presetId,
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        if (!canRead()) {
            return "redirect:/admin/dashboard";
        }
        var reqAttrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (reqAttrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
            jakarta.servlet.http.HttpServletRequest req = sra.getRequest();
            if (req.getRequestURI() != null && req.getRequestURI().contains("/admin/review")) {
                String qOrderId = req.getParameter("orderId") != null ? req.getParameter("orderId")
                        : (req.getParameter("order_id") != null ? req.getParameter("order_id") : req.getParameter("orderNumber"));
                String qCustId = req.getParameter("customerId") != null ? req.getParameter("customerId") : req.getParameter("customer_id");
                String qCustName = req.getParameter("customerName") != null ? req.getParameter("customerName")
                        : (req.getParameter("customer_name") != null ? req.getParameter("customer_name") : req.getParameter("name"));
                String qDate = ExternalIntegrationController.extractDateFromRequest(req, req.getParameter("date"));

                if (externalIntegrationService != null && qOrderId != null && !qOrderId.trim().isEmpty()) {
                    com.twekl.dashboard.dto.administration.ExternalReviewRequest reviewReq = new com.twekl.dashboard.dto.administration.ExternalReviewRequest();
                    reviewReq.setOrderId(qOrderId.trim());
                    if (qCustId != null && !qCustId.trim().isEmpty()) reviewReq.setCustomerId(qCustId.trim());
                    if (qCustName != null && !qCustName.trim().isEmpty()) reviewReq.setCustomerName(qCustName.trim());
                    if (qDate != null && !qDate.trim().isEmpty()) reviewReq.setDate(qDate.trim());
                    externalIntegrationService.createReviewSession(reviewReq, "");
                }
                return "redirect:/followup/review/" + (qOrderId != null && !qOrderId.trim().isEmpty() ? qOrderId.trim() : "");
            }
        }
        model.addAttribute("activeTab", "followups");
        List<TimeFilterPreset> presets = timeFilterService.findAll();
        model.addAttribute("presets", presets);

        String effectiveTimeFilter = "ALL";
        if (timeFilter != null && !timeFilter.trim().isEmpty()) {
            effectiveTimeFilter = timeFilter.trim();
        } else if (preset != null && !preset.trim().isEmpty()) {
            effectiveTimeFilter = preset.trim();
        } else if (presetId != null) {
            for (TimeFilterPreset p : presets) {
                if (presetId.equals(p.getId())) {
                    effectiveTimeFilter = p.getPresetKey() != null ? p.getPresetKey() : p.getName();
                    break;
                }
            }
            if ("ALL".equals(effectiveTimeFilter)) {
                effectiveTimeFilter = String.valueOf(presetId);
            }
        }

        String effectiveStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ALL";

        org.springframework.data.domain.Page<com.twekl.dashboard.dto.administration.CustomerFollowupSummary> initialPage =
                followupService.findPagedSummaries(effectiveTimeFilter, effectiveStatus, null,
                        org.springframework.data.domain.PageRequest.of(0, 40, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
        model.addAttribute("followupSummaries", initialPage.getContent());
        model.addAttribute("totalCustomers", initialPage.getTotalElements());
        model.addAttribute("totalPages", initialPage.getTotalPages());
        model.addAttribute("currentPage", 1);
        model.addAttribute("pageSize", 40);
        if (customerId != null) {
            model.addAttribute("targetCustomerId", customerId);
        }
        return "administration/followup/followups";
    }

    @Override
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
        if ("externalId".equalsIgnoreCase(sortField)) field = "externalId";
        else if ("totalOrders".equalsIgnoreCase(sortField)) field = "totalOrders";
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

        java.util.List<com.twekl.dashboard.dto.administration.CustomerFollowupSummary> content = new java.util.ArrayList<>(pagedResult.getContent());
        if ("satisfaction".equalsIgnoreCase(sortField) || "latestSatisfaction".equalsIgnoreCase(sortField)) {
            java.util.Comparator<com.twekl.dashboard.dto.administration.CustomerFollowupSummary> comp = java.util.Comparator.comparingInt(s -> {
                String sat = s.getLatestSatisfaction();
                if ("SATISFIED".equalsIgnoreCase(sat)) return 3;
                if ("NEUTRAL".equalsIgnoreCase(sat)) return 2;
                if ("UNSATISFIED".equalsIgnoreCase(sat)) return 1;
                return 0;
            });
            if (direction == org.springframework.data.domain.Sort.Direction.DESC) {
                comp = comp.reversed();
            }
            content.sort(comp);
        } else if ("remainingFollowupsCount".equalsIgnoreCase(sortField)) {
            java.util.Comparator<com.twekl.dashboard.dto.administration.CustomerFollowupSummary> comp = java.util.Comparator.comparingInt(com.twekl.dashboard.dto.administration.CustomerFollowupSummary::getRemainingFollowupsCount);
            if (direction == org.springframework.data.domain.Sort.Direction.DESC) {
                comp = comp.reversed();
            }
            content.sort(comp);
        }

        Map<String, Object> responseData = new java.util.LinkedHashMap<>();
        responseData.put("content", content);
        responseData.put("currentPage", page);
        responseData.put("pageSize", pageSize);
        responseData.put("totalElements", pagedResult.getTotalElements());
        responseData.put("totalPages", pagedResult.getTotalPages());
        responseData.put("first", pagedResult.isFirst());
        responseData.put("last", pagedResult.isLast());

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @Override
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

    @Override
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
        String status = body != null ? body.get("status") : null;
        OrderFollowupCheck updated = followupService.updateFollowupDetails(checkId, notes, photoUrl, satisfaction, status);
        return ResponseEntity.ok(ApiResponse.success("Follow-up updated successfully", updated));
    }

    @PostMapping("/{checkId:\\d+}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> updateCheckStatus(
            @PathVariable("checkId") Long checkId,
            @RequestParam("status") String status) {
        if (!canToggleCheck()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to change checkpoint status"));
        }
        OrderFollowupCheck check = followupService.updateCheckStatus(checkId, status);
        return ResponseEntity.ok(ApiResponse.success("Checkpoint status updated to " + check.getStatus(), check));
    }

    @PostMapping("/orders/{orderId:\\d+}/skip")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> skipOrderChecks(@PathVariable("orderId") Long orderId) {
        if (!canToggleCheck()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to update order checks"));
        }
        followupService.markOrderChecksUnfinished(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order checkpoints marked as Unfinished / Skipped", null));
    }

    @PostMapping("/orders/{orderId:\\d+}/unskip")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> unskipOrderChecks(@PathVariable("orderId") Long orderId) {
        if (!canToggleCheck()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to update order checks"));
        }
        followupService.unskipOrderChecks(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order checkpoints reactivated", null));
    }

    @Override
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

    @Override
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

