package com.twekl.dashboard.controller;

import com.twekl.dashboard.model.Customer;
import com.twekl.dashboard.model.CustomerFeedback;
import com.twekl.dashboard.model.CustomerOrder;
import com.twekl.dashboard.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")

public class CustomerApiController {

    private final CustomerService customerService;

    @Autowired
    public CustomerApiController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<Customer> getAllCustomers(@RequestParam(value = "filter", required = false, defaultValue = "all") String filter) {
        return customerService.getCustomersByFilter(filter);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/orders")
    public List<CustomerOrder> getCustomerOrders(@PathVariable Long id) {
        return customerService.getCustomerOrders(id);
    }

    @PostMapping("/{id}/orders")
    public CustomerOrder addCustomerOrder(@PathVariable Long id, @RequestBody CustomerOrder order) {
        return customerService.addCustomerOrder(id, order);
    }

    @GetMapping("/{id}/feedbacks")
    public List<CustomerFeedback> getCustomerFeedbacks(@PathVariable Long id) {
        return customerService.getCustomerFeedbacks(id);
    }

    @PostMapping("/{id}/feedbacks")
    public CustomerFeedback addCustomerFeedback(@PathVariable Long id, @RequestBody CustomerFeedback feedback) {
        return customerService.addCustomerFeedback(id, feedback);
    }

    @GetMapping("/stats")
    public Map<String, Object> getCustomerFollowupStats() {
        return customerService.getCustomerFollowupStats();
    }
}
