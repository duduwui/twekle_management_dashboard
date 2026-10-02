package com.twekl.dashboard.modules.customer.controller;

import com.twekl.dashboard.common.dto.ApiResponse;
import com.twekl.dashboard.modules.customer.entity.Customer;
import com.twekl.dashboard.modules.customer.entity.CustomerFeedback;
import com.twekl.dashboard.modules.customer.entity.CustomerOrder;
import com.twekl.dashboard.modules.customer.entity.OrderFollowupCheck;
import com.twekl.dashboard.modules.customer.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/internal", "/api"})
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> getAllCustomers(@RequestParam(name = "timeFilter", required = false) String timeFilter) {
        return ResponseEntity.ok(customerService.findAll(timeFilter));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/customers/{id}/orders")
    public ResponseEntity<List<CustomerOrder>> getCustomerOrders(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findOrdersByCustomerId(id));
    }

    @GetMapping("/customers/{id}/feedbacks")
    public ResponseEntity<List<CustomerFeedback>> getCustomerFeedbacks(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findFeedbacksByCustomerId(id));
    }

    @GetMapping("/orders/{orderId}/followups")
    public ResponseEntity<List<OrderFollowupCheck>> getOrderFollowups(@PathVariable Long orderId) {
        return ResponseEntity.ok(customerService.findFollowupsByOrderId(orderId));
    }

    @PatchMapping("/followups/{id}/status")
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> toggleFollowupStatus(@PathVariable Long id) {
        OrderFollowupCheck updated = customerService.toggleFollowupStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Status updated", updated));
    }

    @PutMapping("/followups/{id}")
    public ResponseEntity<ApiResponse<OrderFollowupCheck>> updateFollowup(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        OrderFollowupCheck updated = customerService.updateFollowupDetails(id, body.get("notes"), body.get("photoUrl"));
        return ResponseEntity.ok(ApiResponse.success("Follow-up updated", updated));
    }

    @GetMapping("/customers/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(customerService.getStats());
    }
}
