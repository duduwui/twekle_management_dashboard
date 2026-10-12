package com.twekl.dashboard.controller.administrative;

import com.twekl.dashboard.model.order.CustomerOrder;
import com.twekl.dashboard.model.order.OrderFollowupCheck;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.service.administrative.CustomerService;
import com.twekl.dashboard.service.administrative.FollowupService;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Canonical route /admin/orders with @Autowired.
 */
@RestController
@RequestMapping("/admin/orders")
public class OrderController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private FollowupService followupService;

    public OrderController() {
    }

    @Autowired
    public OrderController(CustomerService customerService, FollowupService followupService) {
        this.customerService = customerService;
        this.followupService = followupService;
    }

    @GetMapping(value = "/{orderId:\\d+}/followups", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderFollowupCheck>>> getOrderFollowupChecks(@PathVariable("orderId") Long orderId) {
        List<OrderFollowupCheck> checks = followupService.findChecksByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(checks));
    }

    @GetMapping(value = "/{orderId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerOrder>> getOrderById(@PathVariable("orderId") Long orderId) {
        return customerService.findOrderById(orderId)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order not found with ID: " + orderId)));
    }
}
