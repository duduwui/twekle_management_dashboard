package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CustomerFollowupSummary;
import com.twekl.dashboard.dto.administration.FollowupCheckRequest;
import com.twekl.dashboard.exception.ResourceNotFoundException;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.repository.administration.CustomerOrderRepository;
import com.twekl.dashboard.repository.administration.CustomerRepository;
import com.twekl.dashboard.repository.administration.OrderFollowupCheckRepository;
import com.twekl.dashboard.repository.administration.TimeFilterPresetRepository;
import com.twekl.dashboard.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for managing customer follow-up milestones and order checks.
 * Consolidated single service file with final autowired dependencies.
 */
@Service
@Transactional
public class FollowupService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;

    @Autowired
    public FollowupService(final CustomerRepository customerRepository,
                           final CustomerOrderRepository orderRepository,
                           final OrderFollowupCheckRepository followupCheckRepository,
                           final TimeFilterPresetRepository timeFilterPresetRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CustomerFollowupSummary> findPagedSummaries(
            String timeFilter,
            String statusFilter,
            String search,
            org.springframework.data.domain.Pageable pageable) {
        return findPagedSummaries(timeFilter, statusFilter, search, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CustomerFollowupSummary> findPagedSummaries(
            String timeFilter,
            String statusFilter,
            String search,
            Instant dateFrom,
            Instant dateTo,
            org.springframework.data.domain.Pageable pageable) {

        Instant cutoff = DateUtil.getFilterCutoff(timeFilter);
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String status = (statusFilter != null) ? statusFilter.trim().toUpperCase() : "ALL";

        boolean isAlert = "ALERT".equals(status) || "OVERDUE".equals(status) || "NEEDS_ACTION".equals(status) || "ACTION".equals(status);
        boolean isPending = "IDLE".equals(status) || "PENDING".equals(status) || "UPCOMING".equals(status) || "IN_QUEUE".equals(status) || "SCHEDULED".equals(status);
        boolean isCompleted = "DONE".equals(status) || "COMPLETED".equals(status) || "FINISHED".equals(status);

        org.springframework.data.domain.Page<Customer> customerPage;
        if (cleanSearch != null) {
            customerPage = (cutoff == null)
                    ? customerRepository.searchCustomers(cleanSearch, pageable)
                    : customerRepository.searchCustomersWithCutoff(cleanSearch, cutoff, pageable);
        } else if (dateFrom != null && dateTo != null) {
            customerPage = customerRepository.findByCreatedAtBetween(dateFrom, dateTo, pageable);
        } else if (isAlert) {
            customerPage = (cutoff == null)
                    ? customerRepository.findOverdueCustomers(pageable)
                    : customerRepository.findOverdueCustomersWithCutoff(cutoff, pageable);
        } else if (isPending) {
            customerPage = (cutoff == null)
                    ? customerRepository.findInQueueCustomers(pageable)
                    : customerRepository.findInQueueCustomersWithCutoff(cutoff, pageable);
        } else if (isCompleted) {
            customerPage = (cutoff == null)
                    ? customerRepository.findCompletedCustomers(pageable)
                    : customerRepository.findCompletedCustomersWithCutoff(cutoff, pageable);
        } else {
            customerPage = (cutoff == null)
                    ? customerRepository.findAll(pageable)
                    : customerRepository.findByCreatedAtAfter(cutoff, pageable);
        }

        if (customerPage == null || customerPage.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(
                    Collections.emptyList(), pageable, customerPage != null ? customerPage.getTotalElements() : 0);
        }

        List<Customer> customers = customerPage.getContent();
        List<Long> customerIds = customers.stream().map(Customer::getId).toList();

        List<TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getName() != null) activePresetNames.add(p.getName().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        List<CustomerOrder> allOrders = orderRepository.findByCustomerIds(customerIds);
        List<OrderFollowupCheck> allChecks = followupCheckRepository.findByCustomerIds(customerIds);

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

        Instant now = Instant.now();
        List<CustomerFollowupSummary> results = new ArrayList<>();
        for (Customer c : customers) {
            List<CustomerOrder> orders = ordersByCustId.getOrDefault(c.getId(), Collections.emptyList());
            c.setTotalOrders(orders.size());
            double totalSpent = orders.stream().mapToDouble(CustomerOrder::getTotalAmount).sum();
            c.setTotalSpent(totalSpent);

            CustomerFollowupSummary summary = buildSummary(c, orders, checksByOrderId, activePresetIds, activePresetNames, now);
            results.add(summary);
        }

        return new org.springframework.data.domain.PageImpl<>(results, pageable, customerPage.getTotalElements());
    }

    @Cacheable(value = "followupSummaries", key = "#timeFilter != null ? #timeFilter : 'ALL'", sync = true)
    @Transactional(readOnly = true)
    public List<CustomerFollowupSummary> findAllSummaries(String timeFilter) {
        Instant cutoff = DateUtil.getFilterCutoff(timeFilter);
        List<Customer> allCustomers;
        if (cutoff == null) {
            org.springframework.data.domain.Page<Customer> page = customerRepository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, 200, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
            if (page != null && page.getContent() != null) {
                allCustomers = page.getContent();
            } else {
                allCustomers = customerRepository.findAll();
            }
        } else {
            List<Customer> paged = customerRepository.findByLastOrderDateAfter(cutoff,
                    org.springframework.data.domain.PageRequest.of(0, 200, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastOrderDate")));
            if (paged != null && !paged.isEmpty()) {
                allCustomers = paged;
            } else {
                allCustomers = customerRepository.findByLastOrderDateAfter(cutoff);
            }
        }
        if (allCustomers == null || allCustomers.isEmpty()) return List.of();
        if (allCustomers.size() > 200) {
            allCustomers = allCustomers.subList(0, 200);
        }
        List<Long> customerIds = allCustomers.stream().map(Customer::getId).toList();
        List<TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();

        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getName() != null) activePresetNames.add(p.getName().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        List<CustomerOrder> allOrders = orderRepository.findByCustomerIds(customerIds);
        List<OrderFollowupCheck> allChecks = followupCheckRepository.findByCustomerIds(customerIds);

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

        Instant now = Instant.now();
        List<CustomerFollowupSummary> results = new ArrayList<>();

        for (Customer c : allCustomers) {
            List<CustomerOrder> orders = ordersByCustId.getOrDefault(c.getId(), Collections.emptyList());
            c.setTotalOrders(orders.size());
            double totalSpent = orders.stream().mapToDouble(CustomerOrder::getTotalAmount).sum();
            c.setTotalSpent(totalSpent);

            CustomerFollowupSummary summary = buildSummary(c, orders, checksByOrderId, activePresetIds, activePresetNames, now);
            results.add(summary);
        }

        return results;
    }

    private CustomerFollowupSummary buildSummary(Customer c,
                                                 List<CustomerOrder> orders,
                                                 Map<Long, List<OrderFollowupCheck>> checksByOrderId,
                                                 Set<Long> activePresetIds,
                                                 Set<String> activePresetNames, Instant now) {
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
            List<OrderFollowupCheck> checks = checksByOrderId.getOrDefault(ord.getId(), Collections.emptyList());

            for (OrderFollowupCheck ch : checks) {
                boolean isPresetActive = (ch.getPreset() != null && activePresetIds.contains(ch.getPreset().getId()))
                        || (ch.getMilestoneName() != null && activePresetNames.contains(ch.getMilestoneName().trim().toLowerCase()));
                if (!isPresetActive) {
                    continue;
                }

                if (!ch.isCompleted()) {
                    pendingCount++;
                    Instant dueTime = calculateDueTime(ord.getOrderDate(), ch.getDurationUnit(), ch.getDurationValue());
                    if (dueTime != null && now.isAfter(dueTime)) {
                        if (mostOverdueCheck == null || dueTime.isBefore(earliestOverdueTime)) {
                            mostOverdueCheck = ch;
                            earliestOverdueTime = dueTime;
                            overdueOrderId = ord.getId();
                        }
                    } else if (dueTime != null) {
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
            summary.setNextPendingMinutesLeft((int) java.time.Duration.between(now, earliestOverdueTime).toMinutes());
        } else {
            summary.setFollowupStatus("IDLE");
            summary.setAllFollowupsCompleted(false);
            summary.setNextPendingFollowup(earliestUpcomingCheck != null ? earliestUpcomingCheck.getMilestoneName() : "Pending");
            summary.setNextPendingOrderId(upcomingOrderId);
            summary.setNextPendingMinutesLeft(earliestUpcomingTime != null ? (int) java.time.Duration.between(now, earliestUpcomingTime).toMinutes() : null);
        }

        return summary;
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

        if (req != null) {
            if (req.getNotes() != null) check.setNotes(req.getNotes());
            if (req.getImageUrl() != null) check.setImageUrl(req.getImageUrl());
            if (req.getCheckedBy() != null) check.setCheckedBy(req.getCheckedBy());
            if (req.getSatisfaction() != null) check.setSatisfaction(req.getSatisfaction());
        }

        return followupCheckRepository.save(check);
    }

    public static Instant calculateDueTime(Instant start, String unit, int val) {
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
}
