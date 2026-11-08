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
 * Implements OrderOperations interface contract.
 */
@RestController
@RequestMapping("/admin/orders")
public class OrderController implements OrderOperations {

    private final CustomerService customerService;
    private final FollowupService followupService;
    private final com.twekl.dashboard.repository.administration.CustomerOrderRepository customerOrderRepository;

    @Autowired
    public OrderController(final CustomerService customerService, 
                           final FollowupService followupService,
                           final com.twekl.dashboard.repository.administration.CustomerOrderRepository customerOrderRepository) {
        this.customerService = customerService;
        this.followupService = followupService;
        this.customerOrderRepository = customerOrderRepository;
    }

    @GetMapping(value = "/lookup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> lookupOrder(@org.springframework.web.bind.annotation.RequestParam("orderId") String orderIdentifier) {
        CustomerOrder order = null;
        if (orderIdentifier != null && !orderIdentifier.trim().isEmpty()) {
            String clean = orderIdentifier.trim();
            java.util.List<CustomerOrder> matches = customerOrderRepository.findByOrderNumberWithCustomer(clean);
            if (matches != null && !matches.isEmpty()) {
                order = matches.get(0);
            }
            if (order == null) {
                try {
                    Long numId = Long.parseLong(clean);
                    order = customerOrderRepository.findByIdWithCustomer(numId).orElse(null);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order not found with identifier: " + orderIdentifier));
        }

        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("orderId", order.getId());
        map.put("orderNumber", order.getOrderNumber());
        map.put("customerId", order.getCustomer() != null ? order.getCustomer().getId() : null);
        map.put("customerName", order.getCustomer() != null ? order.getCustomer().getName() : "Customer");
        map.put("orderDate", order.getOrderDate());
        return ResponseEntity.ok(ApiResponse.success(map));
    }

    @Override
    @GetMapping(value = "/{orderId:\\d+}/followups", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<OrderFollowupCheck>>> getOrderFollowupChecks(@PathVariable("orderId") Long orderId) {
        List<OrderFollowupCheck> checks = followupService.findChecksByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(checks));
    }

    @Override
    @GetMapping(value = "/{orderId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CustomerOrder>> getOrderById(@PathVariable("orderId") Long orderId) {
        return customerService.findOrderById(orderId)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order not found with ID: " + orderId)));
    }

    @Override
    @org.springframework.web.bind.annotation.PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CustomerOrder>> createOrder(@org.springframework.web.bind.annotation.RequestBody CustomerOrder order) {
        CustomerOrder saved = customerService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created and follow-up milestones scheduled", saved));
    }
}
