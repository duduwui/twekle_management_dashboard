package com.twekl.dashboard.administration.service.customer;

import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.model.order.CustomerFeedback;
import com.twekl.dashboard.administration.model.order.CustomerOrder;
import com.twekl.dashboard.administration.repository.customer.CustomerRepository;
import com.twekl.dashboard.administration.repository.order.CustomerFeedbackRepository;
import com.twekl.dashboard.administration.repository.order.CustomerOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomerQueryService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;

    public CustomerQueryService(CustomerRepository customerRepository,
                                CustomerOrderRepository orderRepository,
                                CustomerFeedbackRepository feedbackRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public Optional<Customer> findById(Long customerId) {
        return customerRepository.findById(customerId);
    }

    public Optional<Customer> findByCustomerId(Long customerId) {
        return customerRepository.findById(customerId);
    }

    public Optional<CustomerOrder> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<CustomerOrder> findOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
    }

    public List<CustomerFeedback> findFeedbacksByCustomerId(Long customerId) {
        return feedbackRepository.findByCustomerIdOrderByFeedbackDateDesc(customerId);
    }
}
