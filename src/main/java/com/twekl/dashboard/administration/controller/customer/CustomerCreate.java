package com.twekl.dashboard.administration.controller.customer;

import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.response.ApiResponse;
import com.twekl.dashboard.administration.service.customer.CustomerCreateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creation operations for Customers.
 */
@RestController
@RequestMapping("/admin/customers")
public class CustomerCreate {

    private final CustomerCreateService customerCreateService;

    public CustomerCreate(CustomerCreateService customerCreateService) {
        this.customerCreateService = customerCreateService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'PERMISSION_CUSTOMER_FOLLOWUP_CREATE', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@RequestBody Customer customer) {
        Customer created = customerCreateService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", created));
    }
}
