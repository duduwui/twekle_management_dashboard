package com.twekl.dashboard.administration.controller.customer;

import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.customer.CustomerDeleteService;
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
@RequestMapping("/admin/customers")
public class CustomerDelete {

    private final CustomerDeleteService customerDeleteService;

    public CustomerDelete(CustomerDeleteService customerDeleteService) {
        this.customerDeleteService = customerDeleteService;
    }

    @DeleteMapping("/{customerId:\\d+}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CUSTOMER_FOLLOWUP_DELETE', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable("customerId") Long customerId) {
        customerDeleteService.deleteCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }
}
