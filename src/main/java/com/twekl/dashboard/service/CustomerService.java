package com.twekl.dashboard.service;

import com.twekl.dashboard.model.Customer;
import com.twekl.dashboard.model.CustomerFeedback;
import com.twekl.dashboard.model.CustomerOrder;
import com.twekl.dashboard.repository.CustomerFeedbackRepository;
import com.twekl.dashboard.repository.CustomerOrderRepository;
import com.twekl.dashboard.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           CustomerFeedbackRepository feedbackRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> getCustomersByFilter(String filter) {
        if (filter == null || filter.equalsIgnoreCase("all")) {
            return customerRepository.findAll();
        }
        if (filter.equalsIgnoreCase("24h")) {
            return customerRepository.findByDaysSinceLastOrderLessThanEqualOrderByDaysSinceLastOrderAsc(1);
        }
        if (filter.equalsIgnoreCase("7d")) {
            return customerRepository.findByDaysSinceLastOrderBetweenOrderByDaysSinceLastOrderAsc(2, 7);
        }
        if (filter.equalsIgnoreCase("30d")) {
            return customerRepository.findByDaysSinceLastOrderGreaterThanEqualOrderByDaysSinceLastOrderAsc(30);
        }
        return customerRepository.findAll();
    }

    public List<CustomerOrder> getCustomerOrders(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
    }

    public CustomerOrder addCustomerOrder(Long customerId, CustomerOrder order) {
        order.setCustomerId(customerId);
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        CustomerOrder saved = orderRepository.save(order);

        // Update customer summary
        customerRepository.findById(customerId).ifPresent(c -> {
            c.setTotalOrders(c.getTotalOrders() != null ? c.getTotalOrders() + 1 : 1);
            c.setTotalSpent(c.getTotalSpent() != null ? c.getTotalSpent() + (order.getTotalAmount() != null ? order.getTotalAmount() : 0.0) : order.getTotalAmount());
            c.setLastOrderDate(order.getOrderDate());
            c.setDaysSinceLastOrder(0);
            c.setStatus("FOLLOW_UP_24H");
            customerRepository.save(c);
        });

        return saved;
    }

    public List<CustomerFeedback> getCustomerFeedbacks(Long customerId) {
        return feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public CustomerFeedback addCustomerFeedback(Long customerId, CustomerFeedback feedback) {
        feedback.setCustomerId(customerId);
        if (feedback.getCreatedAt() == null) {
            feedback.setCreatedAt(LocalDateTime.now());
        }
        return feedbackRepository.save(feedback);
    }

    public Map<String, Object> getCustomerFollowupStats() {
        List<Customer> all = customerRepository.findAll();
        long total = all.size();
        long count24h = all.stream().filter(c -> c.getDaysSinceLastOrder() != null && c.getDaysSinceLastOrder() <= 1).count();
        long count7d = all.stream().filter(c -> c.getDaysSinceLastOrder() != null && c.getDaysSinceLastOrder() >= 2 && c.getDaysSinceLastOrder() <= 7).count();
        long count30d = all.stream().filter(c -> c.getDaysSinceLastOrder() != null && c.getDaysSinceLastOrder() >= 30).count();

        List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();
        long complimentsCount = allFeedbacks.stream().filter(f -> "COMPLIMENT".equalsIgnoreCase(f.getFeedbackType())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", total);
        stats.put("count24h", count24h);
        stats.put("count7d", count7d);
        stats.put("count30d", count30d);
        stats.put("totalFeedbacks", allFeedbacks.size());
        stats.put("complimentsCount", complimentsCount);
        return stats;
    }
}
