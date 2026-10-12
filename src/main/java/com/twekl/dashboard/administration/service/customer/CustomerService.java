package com.twekl.dashboard.administration.service.customer;

import com.twekl.dashboard.administration.dto.customer.CustomerFollowupSummary;
import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.model.order.CustomerFeedback;
import com.twekl.dashboard.administration.model.order.CustomerOrder;
import com.twekl.dashboard.administration.model.order.OrderFollowupCheck;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.repository.order.CustomerFeedbackRepository;
import com.twekl.dashboard.administration.repository.order.CustomerOrderRepository;
import com.twekl.dashboard.administration.repository.customer.CustomerRepository;
import com.twekl.dashboard.administration.repository.order.OrderFollowupCheckRepository;
import com.twekl.dashboard.administration.repository.timefilter.TimeFilterPresetRepository;
import com.twekl.dashboard.administration.dto.order.FollowupCheckRequest;
import com.twekl.dashboard.administration.util.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;

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

    @Transactional(readOnly = true)
    public List<CustomerFollowupSummary> findAllSummaries(String timeFilter) {
        List<Customer> allCustomers = customerRepository.findAll();
        List<TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();

        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getName() != null) activePresetNames.add(p.getName().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        // Batch load all orders and checks to completely avoid N+1 query loops
        List<CustomerOrder> allOrders = orderRepository.findAll();
        List<OrderFollowupCheck> allChecks = followupCheckRepository.findAll();

        Map<Long, List<CustomerOrder>> ordersByCustId = new HashMap<>();
        for (CustomerOrder o : allOrders) {
            if (o.getCustomer() != null && o.getCustomer().getId() != null) {
                ordersByCustId.computeIfAbsent(o.getCustomer().getId(), k -> new ArrayList<>()).add(o);
            }
        }

        Map<Long, List<OrderFollowupCheck>> checksByOrderId = new HashMap<>();
        for (OrderFollowupCheck ch : allChecks) {
            if (ch.getOrder() != null && ch.getOrder().getId() != null) {
                checksByOrderId.computeIfAbsent(ch.getOrder().getId(), k -> new ArrayList<>()).add(ch);
            }
        }

        Instant cutoff = calculateCutoff(timeFilter);
        List<CustomerFollowupSummary> results = new ArrayList<>();

        for (Customer c : allCustomers) {
            if (cutoff != null && (c.getLastOrderDate() == null || !c.getLastOrderDate().isAfter(cutoff))) {
                continue;
            }

            List<CustomerOrder> orders = ordersByCustId.getOrDefault(c.getId(), Collections.emptyList());
            orders.sort((a, b) -> {
                if (a.getOrderDate() == null && b.getOrderDate() == null) return 0;
                if (a.getOrderDate() == null) return 1;
                if (b.getOrderDate() == null) return -1;
                return b.getOrderDate().compareTo(a.getOrderDate());
            });

            c.setTotalOrders(orders.size());
            double totalSpent = orders.stream().mapToDouble(CustomerOrder::getTotalAmount).sum();
            c.setTotalSpent(totalSpent);

            CustomerFollowupSummary summary = buildSummary(c, orders, checksByOrderId, activePresetIds, activePresetNames);
            results.add(summary);
        }

        return results;
    }

    private CustomerFollowupSummary buildSummary(Customer c,
                                                 List<CustomerOrder> orders,
                                                 Map<Long, List<OrderFollowupCheck>> checksByOrderId,
                                                 Set<Long> activePresetIds,
                                                 Set<String> activePresetNames) {
        CustomerFollowupSummary summary = new CustomerFollowupSummary();
        summary.setCustomer(c);

        if (orders.isEmpty()) {
            summary.setFollowupStatus("IDLE");
            summary.setNextPendingFollowup("-");
            summary.setAllFollowupsCompleted(false);
            summary.setRemainingFollowupsCount(0);
            return summary;
        }

        int pendingCount = 0;
        OrderFollowupCheck mostOverdueCheck = null;
        Instant earliestOverdueTime = null;
        Long overdueOrderId = null;

        OrderFollowupCheck earliestUpcomingCheck = null;
        Instant earliestUpcomingTime = null;
        Long upcomingOrderId = null;

        for (CustomerOrder ord : orders) {
            List<OrderFollowupCheck> checks = new ArrayList<>(checksByOrderId.getOrDefault(ord.getId(), Collections.emptyList()));
            checks.sort(Comparator.comparingInt(OrderFollowupCheck::getEquivalentDurationMinutes));

            for (OrderFollowupCheck ch : checks) {
                boolean isPresetActive = (ch.getPreset() != null && activePresetIds.contains(ch.getPreset().getId()))
                        || (ch.getMilestoneName() != null && activePresetNames.contains(ch.getMilestoneName().trim().toLowerCase()));
                if (!isPresetActive) {
                    continue; // Skip inactive presets and check the nearest active filter time
                }

                if (!ch.isCompleted()) {
                    pendingCount++;
                    Instant dueTime = calculateDueTime(ord.getOrderDate(), ch.getDurationUnit(), ch.getDurationValue());
                    if (dueTime != null && Instant.now().isAfter(dueTime)) {
                        // Overdue: pick the milestone that has been overdue the longest (earliest due time)
                        if (mostOverdueCheck == null || dueTime.isBefore(earliestOverdueTime)) {
                            mostOverdueCheck = ch;
                            earliestOverdueTime = dueTime;
                            overdueOrderId = ord.getId();
                        }
                    } else if (dueTime != null) {
                        // Upcoming: pick the milestone due soonest in the future
                        if (earliestUpcomingCheck == null || dueTime.isBefore(earliestUpcomingTime)) {
                            earliestUpcomingCheck = ch;
                            earliestUpcomingTime = dueTime;
                            upcomingOrderId = ord.getId();
                        }
                    }
                }
            }
        }

        summary.setRemainingFollowupsCount(pendingCount);

        if (pendingCount == 0) {
            summary.setFollowupStatus("DONE");
            summary.setAllFollowupsCompleted(true);
            summary.setNextPendingFollowup("All Done");
            summary.setNextPendingOrderId(null);
            summary.setNextPendingMinutesLeft(null);
        } else if (mostOverdueCheck != null) {
            summary.setFollowupStatus("ALERT");
            summary.setAllFollowupsCompleted(false);
            summary.setNextPendingFollowup(mostOverdueCheck.getMilestoneName());
            summary.setNextPendingOrderId(overdueOrderId);
            summary.setNextPendingMinutesLeft((int) java.time.Duration.between(Instant.now(), earliestOverdueTime).toMinutes());
        } else {
            summary.setFollowupStatus("IDLE");
            summary.setAllFollowupsCompleted(false);
            summary.setNextPendingFollowup(earliestUpcomingCheck != null ? earliestUpcomingCheck.getMilestoneName() : "Pending");
            summary.setNextPendingOrderId(upcomingOrderId);
            summary.setNextPendingMinutesLeft(earliestUpcomingTime != null ? (int) java.time.Duration.between(Instant.now(), earliestUpcomingTime).toMinutes() : null);
        }

        return summary;
    }

    private boolean isCheckDue(CustomerOrder order, OrderFollowupCheck check) {
        if (check.isCompleted() || order == null || order.getOrderDate() == null) return false;
        Instant dueTime = calculateDueTime(order.getOrderDate(), check.getDurationUnit(), check.getDurationValue());
        return Instant.now().isAfter(dueTime);
    }

    private Integer getMinutesUntilDue(CustomerOrder order, OrderFollowupCheck check) {
        if (check.isCompleted() || order == null || order.getOrderDate() == null) return null;
        Instant dueTime = calculateDueTime(order.getOrderDate(), check.getDurationUnit(), check.getDurationValue());
        return (int) java.time.Duration.between(Instant.now(), dueTime).toMinutes();
    }

    private Instant calculateDueTime(Instant start, String unit, int val) {
        if (start == null) return Instant.now();
        String u = unit != null ? unit.toUpperCase() : "DAYS";
        int v = val > 0 ? val : 1;
        return switch (u) {
            case "HOURS" -> start.plus(v, ChronoUnit.HOURS);
            case "WEEKS" -> start.plus(v * 7L, ChronoUnit.DAYS);
            case "MONTHS" -> start.plus(v * 30L, ChronoUnit.DAYS);
            default -> start.plus(v, ChronoUnit.DAYS);
        };
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

    @Transactional
    public List<OrderFollowupCheck> findChecksByOrderId(Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return Collections.emptyList();

        List<TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        List<OrderFollowupCheck> existingChecks = followupCheckRepository.findByOrderId(orderId);

        Map<Long, OrderFollowupCheck> byPresetId = new HashMap<>();
        Map<String, OrderFollowupCheck> byName = new HashMap<>();
        for (OrderFollowupCheck ch : existingChecks) {
            if (ch.getPreset() != null && ch.getPreset().getId() != null) {
                byPresetId.put(ch.getPreset().getId(), ch);
            }
            if (ch.getMilestoneName() != null) {
                byName.put(ch.getMilestoneName().trim().toLowerCase(), ch);
            }
        }

        List<OrderFollowupCheck> result = new ArrayList<>();
        for (TimeFilterPreset preset : activePresets) {
            OrderFollowupCheck check = byPresetId.get(preset.getId());
            if (check == null && preset.getName() != null) {
                check = byName.get(preset.getName().trim().toLowerCase());
            }
            if (check == null) {
                check = new OrderFollowupCheck(order, preset);
                check = followupCheckRepository.save(check);
            } else {
                if (check.getPreset() == null) {
                    check.setPreset(preset);
                }
                check.setMilestoneName(preset.getName());
                check.setDurationValue(preset.getDurationValue());
                check.setDurationUnit(preset.getDurationUnit());
                check.setTargetDays(preset.getTargetDays());
                check = followupCheckRepository.save(check);
            }
            result.add(check);
        }

        for (OrderFollowupCheck ch : result) {
            Instant dueTime = calculateDueTime(order.getOrderDate(), ch.getDurationUnit(), ch.getDurationValue());
            if (dueTime != null) {
                int mins = (int) java.time.Duration.between(Instant.now(), dueTime).toMinutes();
                ch.setMinutesUntilDue(mins);
                ch.setIsDue(!ch.isCompleted() && Instant.now().isAfter(dueTime));
            }
        }

        result.sort(Comparator.comparingInt(OrderFollowupCheck::getEquivalentDurationMinutes));
        return result;
    }


    @Transactional(readOnly = true)
    public List<CustomerFeedback> findFeedbacksByCustomerId(Long customerId) {
        return feedbackRepository.findByCustomerIdOrderByFeedbackDateDesc(customerId);
    }

    @Transactional
    public OrderFollowupCheck toggleFollowupStatus(Long checkId) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found with ID: " + checkId));

        if (check.isCompleted()) {
            check.setCompleted(false);
            check.setStatus("PENDING");
            check.setCompletedDate(null);
        } else {
            check.setCompleted(true);
            check.setStatus("COMPLETED");
            check.setCompletedDate(Instant.now());
        }
        return followupCheckRepository.save(check);
    }

    @Transactional
    public OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl, String satisfaction) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found with ID: " + checkId));

        if (notes != null) check.setNotes(notes);
        if (photoUrl != null) check.setImageUrl(photoUrl);

        if (satisfaction != null && !satisfaction.trim().isEmpty()) {
            check.setSatisfaction(satisfaction.toUpperCase());
        } else if (check.getSatisfaction() == null && ((notes != null && !notes.trim().isEmpty()) || (photoUrl != null && !photoUrl.trim().isEmpty()))) {
            check.setSatisfaction("NEUTRAL");
        }

        Instant now = Instant.now();
        check.setCheckedAt(now);
        check.setUpdatedAt(now);
        if (check.getCheckedBy() == null || check.getCheckedBy().trim().isEmpty()) {
            check.setCheckedBy("Admin");
        }

        return followupCheckRepository.save(check);
    }

    @Transactional
    public OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl) {
        return updateFollowupDetails(checkId, notes, photoUrl, null);
    }

    @Transactional
    public OrderFollowupCheck completeCheck(Long checkId, FollowupCheckRequest req) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up check not found with ID: " + checkId));

        check.setCompleted(true);
        check.setCompletedDate(Instant.now());
        check.setCheckedAt(Instant.now());
        check.setStatus("COMPLETED");

        if (req.getNotes() != null) check.setNotes(req.getNotes());
        if (req.getImageUrl() != null) check.setImageUrl(req.getImageUrl());
        if (req.getCheckedBy() != null) check.setCheckedBy(req.getCheckedBy());
        if (req.getSatisfaction() != null) check.setSatisfaction(req.getSatisfaction());

        return followupCheckRepository.save(check);
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer updated) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));

        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getPhoneNumber() != null) existing.setPhoneNumber(updated.getPhoneNumber());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        if (updated.getCity() != null) existing.setCity(updated.getCity());

        return customerRepository.save(existing);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
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

        List<OrderFollowupCheck> allChecksList = followupCheckRepository.findAll().stream().filter(c -> {
            return (c.getPreset() != null && activePresetIds.contains(c.getPreset().getId()))
                    || (c.getMilestoneName() != null && activePresetNames.contains(c.getMilestoneName().trim().toLowerCase()));
        }).collect(Collectors.toList());

        long totalFollowups = allChecksList.size();
        long completedFollowups = allChecksList.stream().filter(OrderFollowupCheck::isCompleted).count();
        long pendingFollowups = totalFollowups - completedFollowups;

        long overdueFollowups = allChecksList.stream().filter(c -> {
            if (c.isCompleted() || c.getOrder() == null || c.getOrder().getOrderDate() == null) return false;
            Instant dueTime = calculateDueTime(c.getOrder().getOrderDate(), c.getDurationUnit(), c.getDurationValue());
            return dueTime != null && now.isAfter(dueTime);
        }).count();
        long inQueueFollowups = pendingFollowups - overdueFollowups;
        if (inQueueFollowups < 0) inQueueFollowups = 0;

        List<CustomerFollowupSummary> summaries = findAllSummaries("ALL");
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

    private Instant calculateCutoff(String filter) {
        return DateUtil.getFilterCutoff(filter);
    }
}

