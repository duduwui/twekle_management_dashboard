package com.twekl.dashboard.service;

import com.twekl.dashboard.model.Customer;
import com.twekl.dashboard.model.CustomerFeedback;
import com.twekl.dashboard.model.CustomerOrder;
import com.twekl.dashboard.model.OrderFollowupCheck;
import com.twekl.dashboard.model.TimeFilterPreset;
import com.twekl.dashboard.repository.CustomerFeedbackRepository;
import com.twekl.dashboard.repository.CustomerOrderRepository;
import com.twekl.dashboard.repository.CustomerRepository;
import com.twekl.dashboard.repository.OrderFollowupCheckRepository;
import com.twekl.dashboard.repository.TimeFilterPresetRepository;
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
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           CustomerFeedbackRepository feedbackRepository,
                           OrderFollowupCheckRepository followupCheckRepository,
                           TimeFilterPresetRepository timeFilterPresetRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = customerRepository.findAll();
        syncCustomerOrderTotals(list);
        return list;
    }

    public Optional<Customer> getCustomerById(Long id) {
        Optional<Customer> opt = customerRepository.findById(id);
        opt.ifPresent(c -> syncCustomerOrderTotals(Collections.singletonList(c)));
        return opt;
    }

    public List<Customer> getCustomersByFilter(String filter) {
        List<Customer> list;
        if (filter == null || filter.equalsIgnoreCase("all")) {
            list = customerRepository.findAll();
        } else if (filter.equalsIgnoreCase("24h")) {
            list = customerRepository.findByDaysSinceLastOrderLessThanEqualOrderByDaysSinceLastOrderAsc(1);
        } else if (filter.equalsIgnoreCase("7d")) {
            list = customerRepository.findByDaysSinceLastOrderBetweenOrderByDaysSinceLastOrderAsc(2, 7);
        } else if (filter.equalsIgnoreCase("30d")) {
            list = customerRepository.findByDaysSinceLastOrderGreaterThanEqualOrderByDaysSinceLastOrderAsc(30);
        } else {
            list = customerRepository.findAll();
        }
        syncCustomerOrderTotals(list);
        return list;
    }

    private void syncCustomerOrderTotals(List<Customer> customers) {
        for (Customer c : customers) {
            List<CustomerOrder> orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(c.getId());
            if (!orders.isEmpty()) {
                c.setTotalOrders(orders.size());
                double sum = orders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
                c.setTotalSpent(sum);
                if (orders.get(0).getOrderDate() != null) {
                    c.setLastOrderDate(orders.get(0).getOrderDate());
                }

                // Check remaining follow-up checks across all customer orders
                int remainingCount = 0;
                int totalChecksCount = 0;
                String closestPendingMilestone = null;
                Long closestPendingOrderId = null;
                String closestPendingOrderNumber = null;

                for (CustomerOrder o : orders) {
                    List<OrderFollowupCheck> checks = getOrderFollowupChecks(o.getId());
                    for (OrderFollowupCheck chk : checks) {
                        totalChecksCount++;
                        if (!Boolean.TRUE.equals(chk.getIsCompleted())) {
                            remainingCount++;
                            if (closestPendingMilestone == null) {
                                closestPendingMilestone = chk.getPresetName();
                                closestPendingOrderId = o.getId();
                                closestPendingOrderNumber = o.getOrderNumber();
                            }
                        }
                    }
                }

                boolean allCompleted = (totalChecksCount > 0 && remainingCount == 0);
                c.setAllFollowupsCompleted(allCompleted);
                c.setRemainingFollowupsCount(remainingCount);
                c.setNextPendingOrderId(closestPendingOrderId);
                c.setNextPendingOrderNumber(closestPendingOrderNumber);

                if (allCompleted) {
                    c.setNextPendingFollowup("All Done");
                } else if (closestPendingMilestone != null) {
                    c.setNextPendingFollowup(closestPendingMilestone);
                } else {
                    c.setNextPendingFollowup("Pending Follow-up");
                }
            } else {
                c.setAllFollowupsCompleted(false);
                c.setRemainingFollowupsCount(0);
                c.setNextPendingFollowup("-");
                c.setNextPendingOrderId(null);
                c.setNextPendingOrderNumber(null);
            }
        }
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

        // Initialize follow-up checks for new order
        initFollowupChecksForOrder(saved.getId());

        return saved;
    }

    public List<OrderFollowupCheck> getOrderFollowupChecks(Long orderId) {
        List<OrderFollowupCheck> existing = followupCheckRepository.findByOrderIdOrderByIdAsc(orderId);
        List<TimeFilterPreset> activePresets = timeFilterPresetRepository.findByIsActiveTrueOrderByIdAsc();
        if (activePresets.isEmpty()) {
            activePresets = timeFilterPresetRepository.findAllByOrderByIdAsc();
        }

        Map<Long, OrderFollowupCheck> existingByPresetId = new HashMap<>();
        Map<String, OrderFollowupCheck> existingByPresetName = new HashMap<>();
        for (OrderFollowupCheck c : existing) {
            if (c.getPresetId() != null) existingByPresetId.put(c.getPresetId(), c);
            if (c.getPresetName() != null) existingByPresetName.put(c.getPresetName().trim().toLowerCase(), c);
        }

        List<OrderFollowupCheck> result = new ArrayList<>();
        for (TimeFilterPreset p : activePresets) {
            OrderFollowupCheck match = existingByPresetId.get(p.getId());
            if (match == null && p.getName() != null) {
                match = existingByPresetName.get(p.getName().trim().toLowerCase());
            }
            if (match != null) {
                match.setPresetName(p.getName());
                match.setDurationValue(p.getDurationValue());
                match.setDurationUnit(p.getDurationUnit());
                result.add(followupCheckRepository.save(match));
            } else {
                OrderFollowupCheck newCheck = OrderFollowupCheck.builder()
                        .orderId(orderId)
                        .presetId(p.getId())
                        .presetName(p.getName())
                        .durationValue(p.getDurationValue())
                        .durationUnit(p.getDurationUnit())
                        .isCompleted(false)
                        .note("")
                        .imageUrl("")
                        .checkedBy("")
                        .build();
                result.add(followupCheckRepository.save(newCheck));
            }
        }
        return result;
    }

    public List<OrderFollowupCheck> initFollowupChecksForOrder(Long orderId) {
        return getOrderFollowupChecks(orderId);
    }

    public OrderFollowupCheck updateOrderFollowupCheck(Long checkId, OrderFollowupCheck updateData) {
        return followupCheckRepository.findById(checkId).map(c -> {
            if (updateData.getIsCompleted() != null) c.setIsCompleted(updateData.getIsCompleted());
            if (updateData.getNote() != null) c.setNote(updateData.getNote());
            if (updateData.getImageUrl() != null) c.setImageUrl(updateData.getImageUrl());
            c.setSatisfaction(updateData.getSatisfaction());
            if (updateData.getCheckedBy() != null) c.setCheckedBy(updateData.getCheckedBy());
            if (Boolean.TRUE.equals(c.getIsCompleted())) {
                if (c.getCheckedAt() == null) c.setCheckedAt(LocalDateTime.now());
            } else {
                c.setCheckedAt(null);
            }
            return followupCheckRepository.save(c);
        }).orElseThrow(() -> new IllegalArgumentException("Follow-up check not found with id: " + checkId));
    }

    public OrderFollowupCheck toggleOrderFollowupCheck(Long checkId, String adminUser) {
        return followupCheckRepository.findById(checkId).map(c -> {
            boolean nextState = !Boolean.TRUE.equals(c.getIsCompleted());
            c.setIsCompleted(nextState);
            if (nextState) {
                c.setCheckedAt(LocalDateTime.now());
                if (adminUser != null && !adminUser.trim().isEmpty()) {
                    c.setCheckedBy(adminUser);
                } else if (c.getCheckedBy() == null || c.getCheckedBy().isEmpty()) {
                    c.setCheckedBy("Admin");
                }
            } else {
                c.setCheckedAt(null);
            }
            return followupCheckRepository.save(c);
        }).orElseThrow(() -> new IllegalArgumentException("Follow-up check not found with id: " + checkId));
    }

    public List<CustomerFeedback> getCustomerFeedbacks(Long customerId) {
        List<CustomerFeedback> list = feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        customerRepository.findById(customerId).ifPresent(c -> {
            for (CustomerFeedback fb : list) {
                if (fb.getCustomerName() == null || fb.getCustomerName().isEmpty()) {
                    fb.setCustomerName(c.getName());
                }
            }
        });
        return list;
    }

    public CustomerFeedback addCustomerFeedback(Long customerId, CustomerFeedback feedback) {
        feedback.setCustomerId(customerId);
        if (feedback.getCustomerName() == null || feedback.getCustomerName().isEmpty()) {
            customerRepository.findById(customerId).ifPresent(c -> feedback.setCustomerName(c.getName()));
        }
        if (feedback.getCreatedAt() == null) {
            feedback.setCreatedAt(LocalDateTime.now());
        }
        if (feedback.getOrderId() != null && (feedback.getOrderNumber() == null || feedback.getOrderNumber().isEmpty())) {
            orderRepository.findById(feedback.getOrderId()).ifPresent(o -> {
                feedback.setOrderNumber(o.getOrderNumber());
                if (feedback.getOrderSummary() == null || feedback.getOrderSummary().isEmpty()) {
                    feedback.setOrderSummary(o.getItemsSummary());
                }
            });
        }
        return feedbackRepository.save(feedback);
    }

    public CustomerFeedback updateCustomerFeedback(Long feedbackId, CustomerFeedback update) {
        return feedbackRepository.findById(feedbackId).map(fb -> {
            if (update.getContent() != null) fb.setContent(update.getContent());
            if (update.getImageUrl() != null) fb.setImageUrl(update.getImageUrl());
            if (update.getAuthorName() != null) fb.setAuthorName(update.getAuthorName());
            fb.setSatisfaction(update.getSatisfaction());
            if (update.getOrderId() != null) {
                fb.setOrderId(update.getOrderId());
                orderRepository.findById(update.getOrderId()).ifPresent(o -> {
                    fb.setOrderNumber(o.getOrderNumber());
                    fb.setOrderSummary(o.getItemsSummary());
                });
            }
            return feedbackRepository.save(fb);
        }).orElseThrow(() -> new IllegalArgumentException("Feedback not found with id: " + feedbackId));
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
