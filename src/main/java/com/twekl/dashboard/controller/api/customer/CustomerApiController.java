package com.twekl.dashboard.controller.api.customer;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Customers & Follow-ups", description = "Customer CRM Lifecycle, Orders, Feedbacks, and Order Follow-up Checkpoints")
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class CustomerApiController {

    private final CustomerService customerService;

    @Autowired
    public CustomerApiController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "List customers", description = "Retrieves customers by filter status (all, 24h, 7d, 30d, 2m, pending, completed)")
    @GetMapping("/customers")
    public List<Customer> getAllCustomers(@RequestParam(value = "filter", required = false, defaultValue = "all") String filter) {
        return customerService.getCustomersByFilter(filter);
    }

    @Operation(summary = "Get customer by ID", description = "Retrieves customer profile and purchase history")
    @GetMapping("/customers/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get customer orders", description = "Retrieves order history for a customer")
    @GetMapping("/customers/{id}/orders")
    public List<CustomerOrder> getCustomerOrders(@PathVariable Long id) {
        return customerService.getCustomerOrders(id);
    }

    @Operation(summary = "Add order for customer", description = "Creates a new customer order and generates its follow-up checkpoints")
    @PostMapping("/customers/{id}/orders")
    public CustomerOrder addCustomerOrder(@PathVariable Long id, @RequestBody CustomerOrder order) {
        return customerService.addCustomerOrder(id, order);
    }

    @Operation(summary = "Get customer feedbacks", description = "Retrieves feedback records and audio voice notes")
    @GetMapping("/customers/{id}/feedbacks")
    public List<CustomerFeedback> getCustomerFeedbacks(@PathVariable Long id) {
        return customerService.getCustomerFeedbacks(id);
    }

    @Operation(summary = "Add customer feedback", description = "Records feedback, satisfaction rating, and voice memo note")
    @PostMapping("/customers/{id}/feedbacks")
    public CustomerFeedback addCustomerFeedback(@PathVariable Long id, @RequestBody CustomerFeedback feedback) {
        return customerService.addCustomerFeedback(id, feedback);
    }

    @Operation(summary = "Update customer feedback", description = "Modifies existing customer feedback notes or rating")
    @PutMapping("/customers/{customerId}/feedbacks/{feedbackId}")
    public CustomerFeedback updateCustomerFeedback(@PathVariable Long customerId,
                                                   @PathVariable Long feedbackId,
                                                   @RequestBody CustomerFeedback feedback) {
        return customerService.updateCustomerFeedback(feedbackId, feedback);
    }

    @Operation(summary = "Get customer follow-up stats", description = "Computes summary breakdown of pending vs completed customer follow-ups")
    @GetMapping("/customers/stats")
    public Map<String, Object> getCustomerFollowupStats() {
        return customerService.getCustomerFollowupStats();
    }

    // Follow-up Checklist for Orders / Products
    @Operation(summary = "Get order follow-up checkpoints", description = "Lists chronological checkpoints (24h, 3d, 7d, 30d, etc.) for an order")
    @GetMapping("/orders/{orderId}/followups")
    public List<OrderFollowupCheck> getOrderFollowupChecks(@PathVariable Long orderId) {
        return customerService.getOrderFollowupChecks(orderId);
    }

    @Operation(summary = "Update follow-up checkpoint", description = "Updates checkpoint notes, photo proof, or satisfaction rating")
    @PutMapping("/orders/{orderId}/followups/{checkId}")
    public OrderFollowupCheck updateOrderFollowupCheck(@PathVariable Long orderId,
                                                       @PathVariable Long checkId,
                                                       @RequestBody OrderFollowupCheck updateData) {
        return customerService.updateOrderFollowupCheck(checkId, updateData);
    }

    @Operation(summary = "Toggle follow-up completion", description = "Toggles checkpoint done status and records timestamp with operator username")
    @PatchMapping("/orders/{orderId}/followups/{checkId}/toggle")
    public OrderFollowupCheck toggleOrderFollowupCheck(@PathVariable Long orderId,
                                                       @PathVariable Long checkId,
                                                       Authentication auth) {
        String username = (auth != null) ? auth.getName() : "Admin";
        return customerService.toggleOrderFollowupCheck(checkId, username);
    }
}
