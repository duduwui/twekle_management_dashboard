package com.twekl.dashboard.administration.controller.customer;

import com.twekl.dashboard.administration.dto.customer.CustomerFollowupSummary;
import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.model.order.CustomerFeedback;
import com.twekl.dashboard.administration.model.order.CustomerOrder;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.customer.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Managing Customers.
 * Standardized under /admin/customers route with explicit customerId parameter naming.
 */
@RestController
@RequestMapping({"/admin/customers", "/admin/api/customers", "/internal/customers", "/api/customers"})
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> getAllCustomers(
            @RequestParam(required = false, defaultValue = "ALL") String timeFilter,
            HttpServletRequest request) {
        List<Map<String, Object>> data = getCustomersData(timeFilter);
        if (request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/internal/")) {
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> getCustomerStats(HttpServletRequest request) {
        Map<String, Object> stats = customerService.getStats();
        if (request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/internal/")) {
            return ResponseEntity.ok(stats);
        }
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
    public ResponseEntity<?> getCustomerOrders(@PathVariable("customerId") Long customerId, HttpServletRequest request) {
        List<CustomerOrder> orders = customerService.findOrdersByCustomerId(customerId);
        if (request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/internal/")) {
            return ResponseEntity.ok(orders);
        }
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping(value = "/{customerId:\\d+}/feedbacks", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomerFeedback>>> getCustomerFeedbacks(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(customerService.findFeedbacksByCustomerId(customerId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CUSTOMER_FOLLOWUP_CREATE', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@RequestBody Customer customer) {
        Customer created = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", created));
    }

    @PutMapping("/{customerId:\\d+}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CUSTOMER_FOLLOWUP_UPDATE', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(@PathVariable("customerId") Long customerId, @RequestBody Customer customer) {
        Customer updated = customerService.updateCustomer(customerId, customer);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", updated));
    }

    @DeleteMapping("/{customerId:\\d+}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CUSTOMER_FOLLOWUP_DELETE', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable("customerId") Long customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
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
