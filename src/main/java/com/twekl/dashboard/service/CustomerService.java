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
import java.util.stream.Collectors;

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

    public static long toMinutes(Integer durationValue, String durationUnit) {
        if (durationValue == null) durationValue = 24;
        String u = durationUnit != null ? durationUnit.trim().toUpperCase() : "HOURS";
        if (u.startsWith("MIN")) return durationValue;
        if (u.startsWith("HOUR")) return durationValue * 60L;
        if (u.startsWith("DAY")) return durationValue * 24L * 60L;
        if (u.startsWith("WEEK")) return durationValue * 7L * 24L * 60L;
        if (u.startsWith("MONTH")) return durationValue * 30L * 24L * 60L;
        return durationValue * 60L;
    }

    public static boolean isMilestoneDue(LocalDateTime orderDate, Integer durationValue, String durationUnit) {
        if (orderDate == null) return false;
        long ageMinutes = Math.max(0, java.time.Duration.between(orderDate, LocalDateTime.now()).toMinutes());
        long thresholdMinutes = toMinutes(durationValue, durationUnit);
        return ageMinutes >= thresholdMinutes;
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
                    long diffDays = java.time.temporal.ChronoUnit.DAYS.between(orders.get(0).getOrderDate().toLocalDate(), java.time.LocalDate.now());
                    c.setDaysSinceLastOrder((int) Math.max(0, diffDays));
                    long diffHours = java.time.Duration.between(orders.get(0).getOrderDate(), LocalDateTime.now()).toHours();
                    c.setHoursSinceLastOrder(Math.max(0, diffHours));
                }

                int duePendingCount = 0;
                int completedCount = 0;
                String closestPendingMilestone = null;
                Long closestPendingOrderId = null;
                String closestPendingOrderNumber = null;
                long earliestHoursUntilDue = Long.MAX_VALUE;

                for (CustomerOrder o : orders) {
                    List<OrderFollowupCheck> checks = getOrderFollowupChecks(o.getId());
                    for (OrderFollowupCheck chk : checks) {
                        if (Boolean.TRUE.equals(chk.getIsCompleted())) {
                            completedCount++;
                        } else if (Boolean.TRUE.equals(chk.getIsDue())) {
                            duePendingCount++;
                            if (closestPendingMilestone == null) {
                                closestPendingMilestone = chk.getPresetName();
                                closestPendingOrderId = o.getId();
                                closestPendingOrderNumber = o.getOrderNumber();
                            }
                        } else {
                            if (chk.getHoursUntilDue() != null && chk.getHoursUntilDue() > 0 && chk.getHoursUntilDue() < earliestHoursUntilDue) {
                                earliestHoursUntilDue = chk.getHoursUntilDue();
                            }
                        }
                    }
                }

                if (duePendingCount > 0) {
                    c.setFollowupStatus("ALERT");
                    c.setAllFollowupsCompleted(false);
                    c.setRemainingFollowupsCount(duePendingCount);
                    c.setNextPendingOrderId(closestPendingOrderId);
                    c.setNextPendingOrderNumber(closestPendingOrderNumber);
                    c.setNextPendingFollowup(closestPendingMilestone != null ? closestPendingMilestone : "Follow-up Due");
                } else if (completedCount > 0) {
                    c.setFollowupStatus("DONE");
                    c.setAllFollowupsCompleted(true);
                    c.setRemainingFollowupsCount(0);
                    c.setNextPendingOrderId(null);
                    c.setNextPendingOrderNumber(null);
                    c.setNextPendingFollowup("All Done");
                } else {
                    c.setFollowupStatus("IDLE");
                    c.setAllFollowupsCompleted(false);
                    c.setRemainingFollowupsCount(0);
                    c.setNextPendingOrderId(null);
                    c.setNextPendingOrderNumber(null);
                    if (earliestHoursUntilDue != Long.MAX_VALUE) {
                        c.setNextPendingFollowup("Fresh Order (Due in " + earliestHoursUntilDue + "h)");
                    } else {
                        c.setNextPendingFollowup("Fresh Order (Idle)");
                    }
                }
            } else {
                c.setFollowupStatus("IDLE");
                c.setAllFollowupsCompleted(false);
                c.setRemainingFollowupsCount(0);
                c.setNextPendingFollowup("-");
                c.setNextPendingOrderId(null);
                c.setNextPendingOrderNumber(null);
            }
        }
    }

    public List<CustomerOrder> getCustomerOrders(Long customerId) {
        List<CustomerOrder> orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
        for (CustomerOrder o : orders) {
            List<OrderFollowupCheck> checks = getOrderFollowupChecks(o.getId());
            boolean hasDuePending = checks.stream().anyMatch(chk -> Boolean.TRUE.equals(chk.getIsDue()) && !Boolean.TRUE.equals(chk.getIsCompleted()));
            boolean hasCompleted = checks.stream().anyMatch(chk -> Boolean.TRUE.equals(chk.getIsCompleted()));
            long ageHours = Math.max(0, java.time.Duration.between(o.getOrderDate() != null ? o.getOrderDate() : o.getCreatedAt(), LocalDateTime.now()).toHours());
            o.setAgeHours(ageHours);

            if (hasDuePending) {
                o.setFollowupStatus("ALERT");
                o.setIsFullyFollowedUp(false);
            } else if (hasCompleted) {
                o.setFollowupStatus("DONE");
                o.setIsFullyFollowedUp(true);
            } else {
                o.setFollowupStatus("IDLE");
                o.setIsFullyFollowedUp(false);
            }
        }
        return orders;
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
            c.setHoursSinceLastOrder(0L);
            c.setStatus("FOLLOW_UP_24H");
            customerRepository.save(c);
        });

        // Initialize follow-up checks for new order
        initFollowupChecksForOrder(saved.getId());

        return saved;
    }

    public List<OrderFollowupCheck> getOrderFollowupChecks(Long orderId) {
        List<OrderFollowupCheck> existing = followupCheckRepository.findByOrderIdOrderByIdAsc(orderId);
        List<TimeFilterPreset> allPresets = timeFilterPresetRepository.findAllByOrderByIdAsc();
        Map<Long, TimeFilterPreset> presetMap = allPresets.stream().collect(Collectors.toMap(TimeFilterPreset::getId, p -> p, (a, b) -> a));

        Map<Long, OrderFollowupCheck> existingByPresetId = new HashMap<>();
        Map<String, OrderFollowupCheck> existingByPresetName = new HashMap<>();
        for (OrderFollowupCheck c : existing) {
            if (c.getPresetId() != null) existingByPresetId.put(c.getPresetId(), c);
            if (c.getPresetName() != null) existingByPresetName.put(c.getPresetName().trim().toLowerCase(), c);
        }

        Optional<CustomerOrder> orderOpt = orderRepository.findById(orderId);
        LocalDateTime orderDate = orderOpt.map(CustomerOrder::getOrderDate).orElse(null);
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }

        List<OrderFollowupCheck> result = new ArrayList<>();
        // 1. Process all ACTIVE presets
        for (TimeFilterPreset p : allPresets) {
            if (!Boolean.TRUE.equals(p.getIsActive())) continue;

            OrderFollowupCheck match = existingByPresetId.get(p.getId());
            if (match == null && p.getName() != null) {
                match = existingByPresetName.get(p.getName().trim().toLowerCase());
            }

            if (match != null) {
                match.setPresetId(p.getId());
                match.setPresetName(p.getName());
                match.setDurationValue(p.getDurationValue());
                match.setDurationUnit(p.getDurationUnit());
                enrichCheck(match, orderDate, true);
                result.add(match);
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
                newCheck = followupCheckRepository.save(newCheck);
                enrichCheck(newCheck, orderDate, true);
                result.add(newCheck);
            }
        }

        // 2. Also keep any existing checkpoint that has been completed (or has notes/images)
        // even if its preset was deactivated, so user never loses their saved completion/review work!
        for (OrderFollowupCheck c : existing) {
            boolean alreadyInResult = result.stream().anyMatch(r -> r.getId().equals(c.getId()));
            if (!alreadyInResult && (Boolean.TRUE.equals(c.getIsCompleted()) || (c.getNote() != null && !c.getNote().trim().isEmpty()))) {
                TimeFilterPreset p = c.getPresetId() != null ? presetMap.get(c.getPresetId()) : null;
                boolean isActive = (p != null && Boolean.TRUE.equals(p.getIsActive()));
                enrichCheck(c, orderDate, isActive);
                result.add(c);
            }
        }

        return result;
    }

    private void enrichCheck(OrderFollowupCheck chk, LocalDateTime orderDate, boolean isPresetActive) {
        chk.setIsPresetActive(isPresetActive);
        long ageMinutes = Math.max(0, java.time.Duration.between(orderDate != null ? orderDate : LocalDateTime.now(), LocalDateTime.now()).toMinutes());
        long thresholdMinutes = toMinutes(chk.getDurationValue(), chk.getDurationUnit());
        boolean isDue = isPresetActive && (ageMinutes >= thresholdMinutes);
        chk.setIsDue(isDue);
        chk.setOrderAgeHours(ageMinutes / 60L);
        if (isPresetActive && !isDue) {
            long minLeft = thresholdMinutes - ageMinutes;
            chk.setHoursUntilDue(Math.max(1L, minLeft / 60L));
        } else {
            chk.setHoursUntilDue(0L);
        }
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
