package com.twekl.dashboard.modules.customer.service;

import com.twekl.dashboard.modules.customer.entity.Customer;
import com.twekl.dashboard.modules.customer.entity.CustomerFeedback;
import com.twekl.dashboard.modules.customer.entity.CustomerOrder;
import com.twekl.dashboard.modules.customer.entity.OrderFollowupCheck;
import com.twekl.dashboard.modules.customer.repository.CustomerFeedbackRepository;
import com.twekl.dashboard.modules.customer.repository.CustomerOrderRepository;
import com.twekl.dashboard.modules.customer.repository.CustomerRepository;
import com.twekl.dashboard.modules.customer.repository.OrderFollowupCheckRepository;
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
    private final OrderFollowupCheckRepository followupCheckRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           CustomerFeedbackRepository feedbackRepository,
                           OrderFollowupCheckRepository followupCheckRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll(String timeFilter) {
        List<Customer> all = customerRepository.findAll();
        if (timeFilter == null || timeFilter.trim().isEmpty() || "ALL".equalsIgnoreCase(timeFilter)) {
            return all;
        }

        LocalDateTime cutoff = calculateCutoff(timeFilter);
        if (cutoff == null) return all;

        List<Customer> filtered = new ArrayList<>();
        for (Customer c : all) {
            if (c.getLastOrderDate() != null && c.getLastOrderDate().isAfter(cutoff)) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<CustomerFeedback> findFeedbacksByCustomerId(Long customerId) {
        return feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<OrderFollowupCheck> findFollowupsByOrderId(Long orderId) {
        return followupCheckRepository.findByOrderId(orderId);
    }

    public OrderFollowupCheck toggleFollowupStatus(Long checkId) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found with id: " + checkId));

        if ("COMPLETED".equalsIgnoreCase(check.getStatus())) {
            check.setStatus("PENDING");
            check.setCompleted(false);
            check.setCompletedDate(null);
        } else {
            check.setStatus("COMPLETED");
            check.setCompleted(true);
            check.setCompletedDate(LocalDateTime.now());
        }
        return followupCheckRepository.save(check);
    }

    public OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found with id: " + checkId));

        if (notes != null) check.setNotes(notes);
        if (photoUrl != null) check.setPhotoUrl(photoUrl);

        return followupCheckRepository.save(check);
    }

    public CustomerFeedback addFeedback(Long customerId, Long orderId, String type, String content, String photoUrl, int rating, String milestoneLabel) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));

        CustomerFeedback feedback = new CustomerFeedback(customer, orderId, type, content, photoUrl, rating, milestoneLabel);
        return feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        long totalCustomers = customerRepository.count();
        long totalOrders = orderRepository.count();
        List<OrderFollowupCheck> allChecks = followupCheckRepository.findAll();

        long completedChecks = allChecks.stream().filter(c -> "COMPLETED".equalsIgnoreCase(c.getStatus())).count();
        long pendingChecks = allChecks.size() - completedChecks;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", totalCustomers);
        stats.put("totalOrders", totalOrders);
        stats.put("completedFollowups", completedChecks);
        stats.put("pendingFollowups", pendingChecks);
        stats.put("totalFollowups", allChecks.size());
        return stats;
    }

    private LocalDateTime calculateCutoff(String presetKey) {
        LocalDateTime now = LocalDateTime.now();
        String upper = presetKey.toUpperCase();
        if (upper.contains("24_HOUR") || upper.contains("1_DAY")) return now.minusHours(24);
        if (upper.contains("3_DAY")) return now.minusDays(3);
        if (upper.contains("7_DAY") || upper.contains("1_WEEK")) return now.minusDays(7);
        if (upper.contains("14_DAY") || upper.contains("2_WEEK")) return now.minusDays(14);
        if (upper.contains("30_DAY") || upper.contains("1_MONTH")) return now.minusDays(30);
        if (upper.contains("3_MONTH")) return now.minusMonths(3);
        if (upper.contains("6_MONTH")) return now.minusMonths(6);
        if (upper.contains("1_YEAR")) return now.minusYears(1);
        return null;
    }
}
