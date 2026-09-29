package com.twekl.dashboard.controller;

import com.twekl.dashboard.model.Customer;
import com.twekl.dashboard.model.CustomerFeedback;
import com.twekl.dashboard.model.CustomerOrder;
import com.twekl.dashboard.model.OrderFollowupCheck;
import com.twekl.dashboard.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class CustomerApiController {

    private final CustomerService customerService;

    @Autowired
    public CustomerApiController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customers")
    public List<Customer> getAllCustomers(@RequestParam(value = "filter", required = false, defaultValue = "all") String filter) {
        return customerService.getCustomersByFilter(filter);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customers/{id}/orders")
    public List<CustomerOrder> getCustomerOrders(@PathVariable Long id) {
        return customerService.getCustomerOrders(id);
    }

    @PostMapping("/customers/{id}/orders")
    public CustomerOrder addCustomerOrder(@PathVariable Long id, @RequestBody CustomerOrder order) {
        return customerService.addCustomerOrder(id, order);
    }

    @GetMapping("/customers/{id}/feedbacks")
    public List<CustomerFeedback> getCustomerFeedbacks(@PathVariable Long id) {
        return customerService.getCustomerFeedbacks(id);
    }

    @PostMapping("/customers/{id}/feedbacks")
    public CustomerFeedback addCustomerFeedback(@PathVariable Long id, @RequestBody CustomerFeedback feedback) {
        return customerService.addCustomerFeedback(id, feedback);
    }

    @GetMapping("/customers/stats")
    public Map<String, Object> getCustomerFollowupStats() {
        return customerService.getCustomerFollowupStats();
    }

    // Follow-up Checklist for Orders / Products
    @GetMapping("/orders/{orderId}/followups")
    public List<OrderFollowupCheck> getOrderFollowupChecks(@PathVariable Long orderId) {
        return customerService.getOrderFollowupChecks(orderId);
    }

    @PutMapping("/orders/{orderId}/followups/{checkId}")
    public OrderFollowupCheck updateOrderFollowupCheck(@PathVariable Long orderId,
                                                       @PathVariable Long checkId,
                                                       @RequestBody OrderFollowupCheck updateData) {
        return customerService.updateOrderFollowupCheck(checkId, updateData);
    }

    @PatchMapping("/orders/{orderId}/followups/{checkId}/toggle")
    public OrderFollowupCheck toggleOrderFollowupCheck(@PathVariable Long orderId,
                                                       @PathVariable Long checkId,
                                                       Authentication auth) {
        String username = (auth != null) ? auth.getName() : "Admin";
        return customerService.toggleOrderFollowupCheck(checkId, username);
    }
}
