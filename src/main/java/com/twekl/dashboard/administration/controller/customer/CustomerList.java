package com.twekl.dashboard.administration.controller.customer;

import com.twekl.dashboard.administration.dto.customer.CustomerFollowupSummary;
import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.model.order.CustomerFeedback;
import com.twekl.dashboard.administration.model.order.CustomerOrder;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.customer.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read and Query operations for Customers.
 * Canonical route /admin/customers without legacy aliases.
 */
@RestController
@RequestMapping("/admin/customers")
public class CustomerList {

    private final CustomerService customerService;

    public CustomerList(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllCustomers(
            @RequestParam(required = false, defaultValue = "ALL") String timeFilter) {
        List<Map<String, Object>> data = getCustomersData(timeFilter);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerStats() {
        Map<String, Object> stats = customerService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping(value = "/followups", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomerFollowupSummary>>> getCustomerFollowups(
            @RequestParam(required = false, defaultValue = "ALL") String timeFilter) {
        return ResponseEntity.ok(ApiResponse.success(customerService.findAllSummaries(timeFilter)));
    }

    @GetMapping(value = "/{customerId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Customer>> getCustomerById(@PathVariable("customerId") Long customerId) {
        return customerService.findByCustomerId(customerId)
                .map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Customer not found with ID: " + customerId)));
    }

    @GetMapping(value = "/{customerId:\\d+}/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomerOrder>>> getCustomerOrders(@PathVariable("customerId") Long customerId) {
        List<CustomerOrder> orders = customerService.findOrdersByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping(value = "/{customerId:\\d+}/feedbacks", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomerFeedback>>> getCustomerFeedbacks(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(customerService.findFeedbacksByCustomerId(customerId)));
    }

    private List<Map<String, Object>> getCustomersData(String timeFilter) {
        return customerService.findAllSummaries(timeFilter).stream().map(s -> {
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
