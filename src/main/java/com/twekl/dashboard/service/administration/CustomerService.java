package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.exception.administration.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.repository.administration.CustomerRepository;
import com.twekl.dashboard.repository.administration.OrderFollowupCheckRepository;
import com.twekl.dashboard.repository.administration.CustomerFeedbackRepository;
import com.twekl.dashboard.repository.administration.CustomerOrderRepository;
import com.twekl.dashboard.repository.administration.TimeFilterPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service for customer lifecycle directory, order history, statistics, and CRUD.
 */
@Service
@Transactional
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private CustomerFeedbackRepository feedbackRepository;

    @Autowired
    private OrderFollowupCheckRepository followupCheckRepository;

    @Autowired
    private TimeFilterPresetRepository timeFilterPresetRepository;

    @Autowired
    private FollowupService followupService;

    public CustomerService() {
    }

    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           CustomerFeedbackRepository feedbackRepository,
                           OrderFollowupCheckRepository followupCheckRepository,
                           TimeFilterPresetRepository timeFilterPresetRepository,
                           FollowupService followupService) {
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

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

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

    public void deleteCustomer(Long customerId) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        customerRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        return getStats(followupService.findAllSummaries("ALL"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(List<CustomerFollowupSummary> summaries) {
        long totalCustomers = customerRepository.count();
        long totalOrders = orderRepository.count();
        Instant now = Instant.now();
        List<TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getName() != null) activePresetNames.add(p.getName().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        List<OrderFollowupCheck> allChecksList = followupCheckRepository.findAllWithOrderCustomerAndPreset().stream().filter(c -> {
            return (c.getPreset() != null && activePresetIds.contains(c.getPreset().getId()))
                    || (c.getMilestoneName() != null && activePresetNames.contains(c.getMilestoneName().trim().toLowerCase()));
        }).collect(Collectors.toList());

        long totalFollowups = allChecksList.size();
        long completedFollowups = allChecksList.stream().filter(OrderFollowupCheck::isCompleted).count();
        long pendingFollowups = totalFollowups - completedFollowups;

        long overdueFollowups = allChecksList.stream().filter(c -> {
            if (c.isCompleted() || c.getOrder() == null || c.getOrder().getOrderDate() == null) return false;
            Instant dueTime = FollowupService.calculateDueTime(c.getOrder().getOrderDate(), c.getDurationUnit(), c.getDurationValue() != null ? c.getDurationValue() : 1);
            return dueTime != null && now.isAfter(dueTime);
        }).count();
        long inQueueFollowups = pendingFollowups - overdueFollowups;
        if (inQueueFollowups < 0) inQueueFollowups = 0;

        long alertFollowups = summaries.stream().filter(s -> "ALERT".equalsIgnoreCase(s.getFollowupStatus())).count();
        long idleFollowups = summaries.stream().filter(s -> "IDLE".equalsIgnoreCase(s.getFollowupStatus())).count();
        long doneFollowups = summaries.stream().filter(s -> "DONE".equalsIgnoreCase(s.getFollowupStatus())).count();

        double totalRevenue = orderRepository.findAll().stream()
                .mapToDouble(CustomerOrder::getTotalAmount)
                .sum();

        List<CustomerFeedback> feedbacks = feedbackRepository.findAll();

        long satisfiedFeedbacks = feedbacks.stream().filter(f -> f.getRating() >= 4).count();
        long neutralFeedbacks = feedbacks.stream().filter(f -> f.getRating() == 3).count();
        long unsatisfiedFeedbacks = feedbacks.stream().filter(f -> f.getRating() <= 2).count();

        long satisfiedChecks = allChecksList.stream()
                .filter(c -> c.getSatisfaction() != null && ("SATISFIED".equalsIgnoreCase(c.getSatisfaction()) || "4".equals(c.getSatisfaction()) || "5".equals(c.getSatisfaction())))
                .count();
        long neutralChecks = allChecksList.stream()
                .filter(c -> c.getSatisfaction() != null && ("NEUTRAL".equalsIgnoreCase(c.getSatisfaction()) || "3".equals(c.getSatisfaction())))
                .count();
        long unsatisfiedChecks = allChecksList.stream()
                .filter(c -> c.getSatisfaction() != null && ("UNSATISFIED".equalsIgnoreCase(c.getSatisfaction()) || "1".equals(c.getSatisfaction()) || "2".equals(c.getSatisfaction())))
                .count();

        long totalRated = feedbacks.size() + (satisfiedChecks + neutralChecks + unsatisfiedChecks);
        long totalSatisfied = satisfiedFeedbacks + satisfiedChecks;
        long totalNeutral = neutralFeedbacks + neutralChecks;
        long totalUnsatisfied = unsatisfiedFeedbacks + unsatisfiedChecks;

        double satisfactionRate = totalRated > 0 ? ((double) totalSatisfied / totalRated * 100.0) : 100.0;
        double feedbackRatingSum = feedbacks.stream().mapToInt(CustomerFeedback::getRating).sum();
        double checkRatingSum = (satisfiedChecks * 5.0) + (neutralChecks * 3.0) + (unsatisfiedChecks * 1.0);
        double avgRating = totalRated > 0 ? ((feedbackRatingSum + checkRatingSum) / totalRated) : 5.0;

        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        List<Customer> allCustomersList = customerRepository.findAll();
        long customersToday = allCustomersList.stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(oneDayAgo))
                .count();

        long customersThisWeek = allCustomersList.stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(sevenDaysAgo))
                .count();

        long customersThisMonth = allCustomersList.stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        long completedPercentage = totalFollowups > 0 ? Math.round(((double) completedFollowups / totalFollowups) * 100.0) : 0L;
        long overduePercentage = totalFollowups > 0 ? Math.round(((double) overdueFollowups / totalFollowups) * 100.0) : 0L;
        long inQueuePercentage = totalFollowups > 0 ? Math.max(0, 100L - completedPercentage - overduePercentage) : 0L;

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
