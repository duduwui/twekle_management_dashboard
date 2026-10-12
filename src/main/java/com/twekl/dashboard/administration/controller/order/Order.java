package com.twekl.dashboard.administration.controller.order;

import com.twekl.dashboard.administration.model.order.CustomerOrder;
import com.twekl.dashboard.administration.model.order.OrderFollowupCheck;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.customer.CustomerService;
import com.twekl.dashboard.administration.service.followup.FollowupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for Customer Orders.
 * Dedicated order domain without Controller suffix.
 */
@RestController
@RequestMapping({"/admin/orders", "/admin/api/orders", "/internal/orders", "/api/orders"})
public class Order {

    private final CustomerService customerService;
    private final FollowupService followupService;

    public Order(CustomerService customerService, FollowupService followupService) {
        this.customerService = customerService;
        this.followupService = followupService;
    }

    @GetMapping(value = "/{orderId:\\d+}/followups", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> getOrderFollowupChecks(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        List<OrderFollowupCheck> checks = followupService.findChecksByOrderId(orderId);
        if (request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/internal/")) {
            return ResponseEntity.ok(checks);
        }
        return ResponseEntity.ok(ApiResponse.success(checks));
    }

    @GetMapping(value = "/{orderId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerOrder>> getOrderById(@PathVariable("orderId") Long orderId) {
        return customerService.findOrderById(orderId)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<CustomerOrder>error("Order not found with ID: " + orderId)));
    }
}
