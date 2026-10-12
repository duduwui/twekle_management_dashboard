package com.twekl.dashboard.administration.controller.customer;

import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.customer.CustomerUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Update operations for Customers.
 */
@RestController
@RequestMapping("/admin/customers")
public class CustomerUpdate {

    private final CustomerUpdateService customerUpdateService;

    public CustomerUpdate(CustomerUpdateService customerUpdateService) {
        this.customerUpdateService = customerUpdateService;
    }

    @PutMapping("/{customerId:\\d+}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CUSTOMER_FOLLOWUP_UPDATE', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(@PathVariable("customerId") Long customerId, @RequestBody Customer customer) {
        Customer updated = customerUpdateService.updateCustomer(customerId, customer);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", updated));
    }
}
