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
    private final com.twekl.dashboard.modules.timefilter.repository.TimeFilterPresetRepository timeFilterPresetRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           CustomerFeedbackRepository feedbackRepository,
                           OrderFollowupCheckRepository followupCheckRepository,
                           com.twekl.dashboard.modules.timefilter.repository.TimeFilterPresetRepository timeFilterPresetRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
    }


    @Transactional(readOnly = true)
    public List<Customer> findAll(String timeFilter) {
        List<Customer> all = customerRepository.findAll();

        List<com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        // Batch-load all orders and checks to eliminate N+1 query loop completely
        List<CustomerOrder> allOrders = orderRepository.findAll();
        List<OrderFollowupCheck> allChecks = followupCheckRepository.findAll();

        Map<Long, List<CustomerOrder>> ordersByCustId = new HashMap<>();
        for (CustomerOrder o : allOrders) {
            if (o.getCustomer() != null && o.getCustomer().getId() != null) {
                ordersByCustId.computeIfAbsent(o.getCustomer().getId(), k -> new ArrayList<>()).add(o);
            }
        }
        for (List<CustomerOrder> ordList : ordersByCustId.values()) {
            ordList.sort((a, b) -> {
                if (a.getOrderDate() == null && b.getOrderDate() == null) return 0;
                if (a.getOrderDate() == null) return 1;
                if (b.getOrderDate() == null) return -1;
                return b.getOrderDate().compareTo(a.getOrderDate());
            });
        }

        Map<Long, List<OrderFollowupCheck>> checksByOrderId = new HashMap<>();
        for (OrderFollowupCheck ch : allChecks) {
            if (ch.getOrder() != null && ch.getOrder().getId() != null) {
                checksByOrderId.computeIfAbsent(ch.getOrder().getId(), k -> new ArrayList<>()).add(ch);
            }
        }

        for (Customer c : all) {
            List<CustomerOrder> orders = ordersByCustId.getOrDefault(c.getId(), Collections.emptyList());
            populateCustomerFollowupMetaWithData(c, orders, checksByOrderId, activePresetIds, activePresetNames);
        }

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
        Optional<Customer> opt = customerRepository.findById(id);
        opt.ifPresent(this::populateCustomerFollowupMeta);
        return opt;
    }

    private void populateCustomerFollowupMeta(Customer c) {
        List<com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }
        populateCustomerFollowupMeta(c, activePresetIds, activePresetNames);
    }

    private void populateCustomerFollowupMeta(Customer c, Set<Long> activePresetIds, Set<String> activePresetNames) {
        if (c == null) return;
        List<CustomerOrder> orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(c.getId());
        c.setTotalOrders(orders.size());
        double totalSpent = orders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
        c.setTotalSpent(totalSpent);

        if (orders.isEmpty()) {
            c.setFollowupStatus("IDLE");
            c.setNextPendingFollowup("-");
            c.setAllFollowupsCompleted(false);
            c.setRemainingFollowupsCount(0);
            return;
        }

        int pendingCount = 0;
        OrderFollowupCheck firstDueCheck = null;
        OrderFollowupCheck firstPendingCheck = null;
        Long targetOrderId = null;

        for (CustomerOrder ord : orders) {
            List<OrderFollowupCheck> checks = followupCheckRepository.findByOrderId(ord.getId());
            for (OrderFollowupCheck ch : checks) {
                ch.setOrder(ord);
                if (!ch.getIsCompleted()) {
                    // Check if preset is active
                    boolean isActive = (ch.getPresetId() != null && activePresetIds.contains(ch.getPresetId()))
                            || (ch.getPresetName() != null && activePresetNames.contains(ch.getPresetName().trim().toLowerCase()));
                    if (!isActive) {
                        continue; // Skip deactivated presets!
                    }

                    pendingCount++;
                    if (firstPendingCheck == null) {
                        firstPendingCheck = ch;
                        targetOrderId = ord.getId();
                    }
                    if (ch.getIsDue() && firstDueCheck == null) {
                        firstDueCheck = ch;
                        targetOrderId = ord.getId();
                    }
                }
            }
        }

        c.setRemainingFollowupsCount(pendingCount);
        c.setNextPendingOrderId(targetOrderId);

        if (pendingCount == 0) {
            c.setFollowupStatus("DONE");
            c.setAllFollowupsCompleted(true);
            c.setNextPendingFollowup("✓ All Done");
            c.setNextPendingMinutesLeft(null);
        } else if (firstDueCheck != null) {
            c.setFollowupStatus("ALERT");
            c.setAllFollowupsCompleted(false);
            c.setNextPendingFollowup(firstDueCheck.getPresetName());
            c.setNextPendingMinutesLeft(firstDueCheck.getMinutesUntilDue()); // will be ≤ 0
        } else {
            c.setFollowupStatus("IDLE");
            c.setAllFollowupsCompleted(false);
            c.setNextPendingFollowup(firstPendingCheck != null ? firstPendingCheck.getPresetName() : "Pending");
            c.setNextPendingMinutesLeft(firstPendingCheck != null ? firstPendingCheck.getMinutesUntilDue() : null);
        }
    }

    private void populateCustomerFollowupMetaWithData(Customer c,
                                                      List<CustomerOrder> orders,
                                                      Map<Long, List<OrderFollowupCheck>> checksByOrderId,
                                                      Set<Long> activePresetIds,
                                                      Set<String> activePresetNames) {
        if (c == null) return;
        c.setTotalOrders(orders.size());
        double totalSpent = orders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
        c.setTotalSpent(totalSpent);

        if (orders.isEmpty()) {
            c.setFollowupStatus("IDLE");
            c.setNextPendingFollowup("-");
            c.setAllFollowupsCompleted(false);
            c.setRemainingFollowupsCount(0);
            return;
        }

        int pendingCount = 0;
        OrderFollowupCheck firstDueCheck = null;
        OrderFollowupCheck firstPendingCheck = null;
        Long targetOrderId = null;

        for (CustomerOrder ord : orders) {
            List<OrderFollowupCheck> checks = checksByOrderId.getOrDefault(ord.getId(), Collections.emptyList());
            for (OrderFollowupCheck ch : checks) {
                ch.setOrder(ord);
                if (!ch.getIsCompleted()) {
                    boolean isActive = (ch.getPresetId() != null && activePresetIds.contains(ch.getPresetId()))
                            || (ch.getPresetName() != null && activePresetNames.contains(ch.getPresetName().trim().toLowerCase()));
                    if (!isActive) {
                        continue;
                    }

                    pendingCount++;
                    if (firstPendingCheck == null) {
                        firstPendingCheck = ch;
                        targetOrderId = ord.getId();
                    }
                    if (ch.getIsDue() && firstDueCheck == null) {
                        firstDueCheck = ch;
                        targetOrderId = ord.getId();
                    }
                }
            }
        }

        c.setRemainingFollowupsCount(pendingCount);
        c.setNextPendingOrderId(targetOrderId);

        if (pendingCount == 0) {
            c.setFollowupStatus("DONE");
            c.setAllFollowupsCompleted(true);
            c.setNextPendingFollowup("✓ All Done");
            c.setNextPendingMinutesLeft(null);
        } else if (firstDueCheck != null) {
            c.setFollowupStatus("ALERT");
            c.setAllFollowupsCompleted(false);
            c.setNextPendingFollowup(firstDueCheck.getPresetName());
            c.setNextPendingMinutesLeft(firstDueCheck.getMinutesUntilDue());
        } else {
            c.setFollowupStatus("IDLE");
            c.setAllFollowupsCompleted(false);
            c.setNextPendingFollowup(firstPendingCheck != null ? firstPendingCheck.getPresetName() : "Pending");
            c.setNextPendingMinutesLeft(firstPendingCheck != null ? firstPendingCheck.getMinutesUntilDue() : null);
        }
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
        List<OrderFollowupCheck> all = followupCheckRepository.findByOrderId(orderId);
        List<com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        List<OrderFollowupCheck> filtered = new ArrayList<>();
        for (OrderFollowupCheck ch : all) {
            // Keep if completed, OR if its preset is currently active
            boolean isActive = (ch.getPresetId() != null && activePresetIds.contains(ch.getPresetId()))
                    || (ch.getPresetName() != null && activePresetNames.contains(ch.getPresetName().trim().toLowerCase()));
            if (ch.getIsCompleted() || isActive) {
                filtered.add(ch);
            }
        }
        return filtered;
    }


    public OrderFollowupCheck toggleFollowupStatus(Long checkId) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found with id: " + checkId));

        if (check.getIsCompleted()) {
            check.setIsCompleted(false);
            check.setStatus("PENDING");
            check.setCompletedDate(null);
        } else {
            check.setIsCompleted(true);
            check.setStatus("COMPLETED");
            check.setCompletedDate(LocalDateTime.now());
        }
        return followupCheckRepository.save(check);
    }

    public OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found with id: " + checkId));

        if (notes != null) check.setNote(notes);
        if (photoUrl != null) check.setImageUrl(photoUrl);

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
        long totalFollowups = followupCheckRepository.count();
        long completedFollowups = followupCheckRepository.countByIsCompletedTrue();
        long pendingFollowups = totalFollowups - completedFollowups;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", totalCustomers);
        stats.put("totalOrders", totalOrders);
        stats.put("completedFollowups", completedFollowups);
        stats.put("pendingFollowups", pendingFollowups);
        stats.put("totalFollowups", totalFollowups);
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
