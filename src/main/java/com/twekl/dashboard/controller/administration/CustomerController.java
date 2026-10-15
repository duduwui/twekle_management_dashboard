package com.twekl.dashboard.controller.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.response.ApiResponse;
import com.twekl.dashboard.service.administration.CustomerService;
import com.twekl.dashboard.service.administration.FollowupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Customer Directory, Lifecycle History, and Statistics.
 * Consolidated resource controller.
 */
@RestController
@RequestMapping("/admin/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final FollowupService followupService;

    @Autowired
    public CustomerController(final CustomerService customerService, final FollowupService followupService) {
        this.customerService = customerService;
        this.followupService = followupService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllCustomers(
            @RequestParam(required = false, defaultValue = "ALL") String timeFilter) {
        List<Map<String, Object>> data = getCustomersData(timeFilter);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerStats() {
        Map<String, Object> stats = customerService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping(value = "/followups", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CustomerFollowupSummary>>> getCustomerFollowups(
            @RequestParam(required = false, defaultValue = "ALL") String timeFilter) {
        return ResponseEntity.ok(ApiResponse.success(followupService.findAllSummaries(timeFilter)));
    }

    @GetMapping(value = "/{customerId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Customer>> getCustomerById(@PathVariable("customerId") Long customerId) {
        return customerService.findByCustomerId(customerId)
                .map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Customer not found with ID: " + customerId)));
    }

    @GetMapping(value = "/{customerId:\\d+}/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CustomerOrder>>> getCustomerOrders(@PathVariable("customerId") Long customerId) {
        List<CustomerOrder> orders = customerService.findOrdersByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping(value = "/{customerId:\\d+}/feedbacks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CustomerFeedback>>> getCustomerFeedbacks(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(customerService.findFeedbacksByCustomerId(customerId)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@RequestBody Customer customer) {
        Customer created = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", created));
    }

    @PutMapping(value = "/{customerId:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(@PathVariable("customerId") Long customerId,
                                                                @RequestBody Customer customer) {
        Customer updated = customerService.updateCustomer(customerId, customer);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", updated));
    }

    @DeleteMapping(value = "/{customerId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable("customerId") Long customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private List<Map<String, Object>> getCustomersData(String timeFilter) {
        return followupService.findAllSummaries(timeFilter).stream().map(s -> {
            Customer c = s.getCustomer();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getCustomerId());
            map.put("customerId", c.getCustomerId());
            map.put("name", c.getName());
            map.put("phoneNumber", c.getPhoneNumber());
            map.put("email", c.getEmail());
            map.put("city", c.getCity());
            map.put("totalOrders", c.getTotalOrders());
            map.put("totalSpent", c.getTotalSpent());
            map.put("lastOrderDate", c.getLastOrderDate());
            map.put("createdAt", c.getCreatedAt());
            map.put("followupStatus", s.getFollowupStatus());
            map.put("nextPendingFollowup", s.getNextPendingFollowup());
            map.put("nextPendingOrderId", s.getNextPendingOrderId());
            map.put("allFollowupsCompleted", s.isAllFollowupsCompleted());
            map.put("remainingFollowupsCount", s.getRemainingFollowupsCount());
            map.put("nextPendingMinutesLeft", s.getNextPendingMinutesLeft());
            return map;
        }).toList();
    }
}
