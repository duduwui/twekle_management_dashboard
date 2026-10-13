package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service contract for customer directory, orders, statistics, and CRUD.
 */
public interface CustomerService {

    List<Customer> findAll();

    Optional<Customer> findById(Long customerId);

    Optional<Customer> findByCustomerId(Long customerId);

    Optional<CustomerOrder> findOrderById(Long orderId);

    List<CustomerOrder> findOrdersByCustomerId(Long customerId);

    List<CustomerFeedback> findFeedbacksByCustomerId(Long customerId);

    long count();

    Customer createCustomer(Customer customer);

    Customer updateCustomer(Long customerId, Customer updated);

    void deleteCustomer(Long customerId);

    Map<String, Object> getStats();

    Map<String, Object> getStats(List<CustomerFollowupSummary> summaries);
}
