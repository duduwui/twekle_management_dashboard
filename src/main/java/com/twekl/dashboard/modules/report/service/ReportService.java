package com.twekl.dashboard.modules.report.service;

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
@Transactional(readOnly = true)
public class ReportService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;

    public ReportService(CustomerRepository customerRepository,
                         CustomerOrderRepository orderRepository,
                         CustomerFeedbackRepository feedbackRepository,
                         OrderFollowupCheckRepository followupCheckRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
    }

    public Map<String, Object> generateReport(String period) {
        LocalDateTime cutoff = getCutoffForPeriod(period);

        List<Customer> allCustomers = customerRepository.findAll();
        List<CustomerOrder> allOrders = orderRepository.findAll();
        List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();
        List<OrderFollowupCheck> allChecks = followupCheckRepository.findAll();

        long newCustomers = allCustomers.stream()
                .filter(c -> cutoff == null || (c.getCreatedAt() != null && c.getCreatedAt().isAfter(cutoff)))
                .count();

        long totalOrdersInPeriod = allOrders.stream()
                .filter(o -> cutoff == null || (o.getOrderDate() != null && o.getOrderDate().isAfter(cutoff)))
                .count();

        double revenueInPeriod = allOrders.stream()
                .filter(o -> cutoff == null || (o.getOrderDate() != null && o.getOrderDate().isAfter(cutoff)))
                .mapToDouble(CustomerOrder::getTotalPrice)
                .sum();

        long completedChecks = allChecks.stream()
                .filter(c -> "COMPLETED".equalsIgnoreCase(c.getStatus()))
                .count();

        double completionRate = allChecks.isEmpty() ? 100.0 : ((double) completedChecks / allChecks.size()) * 100.0;

        double avgRating = allFeedbacks.stream()
                .mapToInt(CustomerFeedback::getRating)
                .average()
                .orElse(5.0);

        Map<String, Long> productBreakdown = new HashMap<>();
        for (CustomerOrder o : allOrders) {
            productBreakdown.put(o.getProductName(), productBreakdown.getOrDefault(o.getProductName(), 0L) + o.getQuantity());
        }

        Map<String, Object> report = new HashMap<>();
        report.put("period", period != null ? period : "monthly");
        report.put("newCustomers", newCustomers);
        report.put("totalOrders", totalOrdersInPeriod);
        report.put("totalRevenue", revenueInPeriod);
        report.put("completionRate", Math.round(completionRate * 10.0) / 10.0);
        report.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        report.put("productBreakdown", productBreakdown);
        report.put("totalFeedbacks", allFeedbacks.size());

        return report;
    }

    private LocalDateTime getCutoffForPeriod(String period) {
        if (period == null) return LocalDateTime.now().minusDays(30);
        String p = period.toLowerCase();
        if (p.contains("day") || p.contains("daily") || p.contains("24h")) return LocalDateTime.now().minusHours(24);
        if (p.contains("week")) return LocalDateTime.now().minusDays(7);
        if (p.contains("month")) return LocalDateTime.now().minusDays(30);
        if (p.contains("year")) return LocalDateTime.now().minusYears(1);
        if (p.contains("all")) return null;
        return LocalDateTime.now().minusDays(30);
    }
}
