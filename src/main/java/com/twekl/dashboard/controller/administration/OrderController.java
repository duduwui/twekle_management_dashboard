package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.service.administration.CustomerService;
import com.twekl.dashboard.service.administration.FollowupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for Customer Orders.
 * Canonical route /admin/orders.
 */
@RestController
@RequestMapping("/admin/orders")
public class OrderController {

    private final CustomerService customerService;
    private final FollowupService followupService;

    @Autowired
    public OrderController(final CustomerService customerService, final FollowupService followupService) {
        this.customerService = customerService;
        this.followupService = followupService;
    }

    @GetMapping(value = "/{orderId:\\d+}/followups", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<OrderFollowupCheck>>> getOrderFollowupChecks(@PathVariable("orderId") Long orderId) {
        List<OrderFollowupCheck> checks = followupService.findChecksByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(checks));
    }

    @GetMapping(value = "/{orderId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CustomerOrder>> getOrderById(@PathVariable("orderId") Long orderId) {
        return customerService.findOrderById(orderId)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order not found with ID: " + orderId)));
    }

    @org.springframework.web.bind.annotation.PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CustomerOrder>> createOrder(@org.springframework.web.bind.annotation.RequestBody CustomerOrder order) {
        CustomerOrder saved = customerService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created and follow-up milestones scheduled", saved));
    }
}
