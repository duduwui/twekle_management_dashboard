package com.twekl.dashboard.administration.controller.customer;

import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.customer.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deletion operations for Customers.
 */
@RestController
@RequestMapping({"/admin/customers", "/admin/api/customers", "/internal/customers", "/api/customers"})
public class CustomerDelete {

    private final CustomerService customerService;

    public CustomerDelete(CustomerService customerService) {
        this.customerService = customerService;
    }

    @DeleteMapping("/{customerId:\\d+}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CUSTOMER_FOLLOWUP_DELETE', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable("customerId") Long customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }
}
