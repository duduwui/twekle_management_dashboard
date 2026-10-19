package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.exception.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.repository.administration.CustomerFeedbackRepository;
import com.twekl.dashboard.repository.administration.CustomerOrderRepository;
import com.twekl.dashboard.repository.administration.CustomerRepository;
import com.twekl.dashboard.repository.administration.OrderFollowupCheckRepository;
import com.twekl.dashboard.repository.administration.TimeFilterPresetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for customer directory, orders, statistics, and CRUD.
 * Consolidated single service file with final autowired dependencies.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;
    private final FollowupService followupService;

    @Autowired
    public CustomerService(final CustomerRepository customerRepository,
                           final CustomerOrderRepository orderRepository,
                           final CustomerFeedbackRepository feedbackRepository,
                           final OrderFollowupCheckRepository followupCheckRepository,
                           final TimeFilterPresetRepository timeFilterPresetRepository,
                           final FollowupService followupService) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
        this.followupService = followupService;
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long customerId) {
        return customerRepository.findById(customerId);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findByCustomerId(Long customerId) {
        return customerRepository.findById(customerId);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerOrder> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<CustomerFeedback> findFeedbacksByCustomerId(Long customerId) {
        return feedbackRepository.findByCustomerIdOrderByFeedbackDateDesc(customerId);
    }

    @Transactional(readOnly = true)
    public long count() {
        return customerRepository.count();
    }

    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    public Customer createCustomer(Customer customer) {
        if (customer.getCreatedAt() == null) {
            customer.setCreatedAt(Instant.now());
        }
        return customerRepository.save(customer);
    }

    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    public Customer updateCustomer(Long customerId, Customer updated) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        if (updated.getName() != null) existing.setName(updated.getName().trim());
        if (updated.getPhoneNumber() != null) existing.setPhoneNumber(updated.getPhoneNumber().trim());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail().trim());
        if (updated.getCity() != null) existing.setCity(updated.getCity().trim());
        existing.setTotalOrders(updated.getTotalOrders());
        existing.setTotalSpent(updated.getTotalSpent());

        return customerRepository.save(existing);
    }

    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    public void deleteCustomer(Long customerId) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        customerRepository.delete(existing);
    }

    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    public CustomerOrder createOrder(CustomerOrder order) {
        if (order.getOrderDate() == null) {
            order.setOrderDate(Instant.now());
        }
        if (order.getOrderNumber() == null || order.getOrderNumber().trim().isEmpty()) {
            order.setOrderNumber("ORD-" + System.currentTimeMillis());
        }
        Customer cust = null;
        Long targetCustId = null;
        if (order.getCustomer() != null) {
            targetCustId = order.getCustomer().getId() != null ? order.getCustomer().getId() : order.getCustomer().getCustomerId();
        }
        if (targetCustId != null) {
            cust = customerRepository.findById(targetCustId).orElse(null);
        }
        if (cust == null) {
            throw new com.twekl.dashboard.exception.BadRequestException("Valid customer ID is required to create an order (customer not found: " + targetCustId + ")");
        }
        order.setCustomer(cust);
        cust.setTotalOrders(cust.getTotalOrders() + 1);
        cust.setTotalSpent(cust.getTotalSpent() + order.getTotalAmount());
        cust.setLastOrderDate(order.getOrderDate());
        customerRepository.save(cust);
        CustomerOrder savedOrder = orderRepository.save(order);
        // Automatically schedule milestone checks for all active presets
        followupService.findChecksByOrderId(savedOrder.getId());
        return savedOrder;
    }

    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    public CustomerFeedback createFeedback(CustomerFeedback feedback) {
        if (feedback.getFeedbackDate() == null) {
            feedback.setFeedbackDate(Instant.now());
        }
        return feedbackRepository.save(feedback);
    }

    @Cacheable(value = "customerStats", sync = true)
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        return getStats(null);
    }

    @Transactional
    public Map<String, Object> getStats(List<CustomerFollowupSummary> summaries) {
        try {
            followupCheckRepository.syncOverdueChecks();
            followupCheckRepository.syncPendingChecks();
        } catch (Exception ignored) {}

        long totalCustomers = customerRepository.count();
        long totalOrders = orderRepository.count();
        Instant now = Instant.now();

        long totalFollowups = followupCheckRepository.count();
        long completedFollowups = followupCheckRepository.countByCompletedTrue();
        long pendingFollowups = totalFollowups > completedFollowups ? totalFollowups - completedFollowups : 0;

        long alertFollowups = 0;
        long idleFollowups = 0;
        long doneFollowups = 0;
        if (summaries != null) {
            alertFollowups = summaries.stream().filter(s -> "ALERT".equalsIgnoreCase(s.getFollowupStatus())).count();
            idleFollowups = summaries.stream().filter(s -> "IDLE".equalsIgnoreCase(s.getFollowupStatus())).count();
            doneFollowups = summaries.stream().filter(s -> "DONE".equalsIgnoreCase(s.getFollowupStatus())).count();
        } else {
            alertFollowups = customerRepository.findOverdueCustomers(org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
            idleFollowups = customerRepository.findInQueueCustomers(org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
            doneFollowups = customerRepository.findCompletedCustomers(org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        }

        long overdueFollowups = alertFollowups;
        long inQueueFollowups = pendingFollowups > overdueFollowups ? pendingFollowups - overdueFollowups : 0;

        double totalRevenue = orderRepository.sumTotalRevenue();

        long satisfiedFeedbacks = feedbackRepository.countSatisfied();
        long neutralFeedbacks = feedbackRepository.countNeutral();
        long unsatisfiedFeedbacks = feedbackRepository.countUnsatisfied();
        double feedbackRatingSum = feedbackRepository.sumRatings();
        long totalFeedbackCount = satisfiedFeedbacks + neutralFeedbacks + unsatisfiedFeedbacks;

        long satisfiedChecks = followupCheckRepository.countSatisfied();
        long neutralChecks = followupCheckRepository.countNeutral();
        long unsatisfiedChecks = followupCheckRepository.countUnsatisfied();

        long totalRated = totalFeedbackCount + (satisfiedChecks + neutralChecks + unsatisfiedChecks);
        long totalSatisfied = satisfiedFeedbacks + satisfiedChecks;
        long totalNeutral = neutralFeedbacks + neutralChecks;
        long totalUnsatisfied = unsatisfiedFeedbacks + unsatisfiedChecks;

        double satisfactionRate = totalRated > 0 ? ((double) totalSatisfied / totalRated * 100.0) : 100.0;
        double checkRatingSum = (satisfiedChecks * 5.0) + (neutralChecks * 3.0) + (unsatisfiedChecks * 1.0);
        double avgRating = totalRated > 0 ? ((feedbackRatingSum + checkRatingSum) / totalRated) : 5.0;

        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        long customersToday = customerRepository.countByCreatedAtAfter(oneDayAgo);
        long customersThisWeek = customerRepository.countByCreatedAtAfter(sevenDaysAgo);
        long customersThisMonth = customerRepository.countByCreatedAtAfter(thirtyDaysAgo);

        double rawComp = totalFollowups > 0 ? ((double) completedFollowups / totalFollowups) * 100.0 : 0.0;
        double rawOverdue = totalFollowups > 0 ? ((double) overdueFollowups / totalFollowups) * 100.0 : 0.0;

        // Guarantee overdue items are visually distinguishable on the progress bar if overdueFollowups > 0
        double overdueDisplayPct = overdueFollowups > 0 ? Math.max(1.5, Math.round(rawOverdue * 10.0) / 10.0) : 0.0;
        double compDisplayPct = Math.round(rawComp * 10.0) / 10.0;
        double inQueueDisplayPct = Math.max(0.0, 100.0 - compDisplayPct - overdueDisplayPct);

        long completedPercentage = Math.round(compDisplayPct);
        long overduePercentage = Math.round(overdueDisplayPct);
        long inQueuePercentage = Math.round(inQueueDisplayPct);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", totalCustomers);
        stats.put("customersToday", customersToday);
        stats.put("customersThisWeek", customersThisWeek);
        stats.put("customersThisMonth", customersThisMonth);
        stats.put("totalOrders", totalOrders);
        stats.put("completedFollowups", completedFollowups);
        stats.put("pendingFollowups", pendingFollowups);
        stats.put("totalFollowups", totalFollowups);
        stats.put("overdueFollowups", overdueFollowups);
        stats.put("inQueueFollowups", inQueueFollowups);
        stats.put("completionPercentage", completedPercentage);
        stats.put("completedPercentage", completedPercentage);
        stats.put("overduePercentage", overduePercentage);
        stats.put("inQueuePercentage", inQueuePercentage);
        stats.put("alertFollowups", alertFollowups);
        stats.put("idleFollowups", idleFollowups);
        stats.put("doneFollowups", doneFollowups);
        stats.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        stats.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        stats.put("satisfactionRate", Math.round(satisfactionRate * 10.0) / 10.0);
        stats.put("unsatisfiedCount", totalUnsatisfied);
        stats.put("satisfiedCount", totalSatisfied);
        stats.put("neutralCount", totalNeutral);
        stats.put("totalRated", totalRated);
        return stats;
    }
}
