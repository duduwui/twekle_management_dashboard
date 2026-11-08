package com.twekl.dashboard.service.administration.impl;

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
import com.twekl.dashboard.service.administration.FollowupService;
import com.twekl.dashboard.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Production implementation of FollowupService.
 * Handles customer follow-up checkpoints, milestone tracking, and overdue status synchronization.
 */
@Service
@Transactional
public class FollowupServiceImpl implements FollowupService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired
    public FollowupServiceImpl(final CustomerRepository customerRepository,
                               final CustomerOrderRepository orderRepository,
                               final OrderFollowupCheckRepository followupCheckRepository,
                               final TimeFilterPresetRepository timeFilterPresetRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduledSyncOverdueChecks() {
        syncOverdueStatuses();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncOverdueStatuses() {
        try {
            int unfinishedUpdated = followupCheckRepository.syncUnfinishedChecks();
            int overdueUpdated = followupCheckRepository.syncOverdueChecks();
            int pendingUpdated = followupCheckRepository.syncPendingChecks();
            if ((overdueUpdated > 0 || unfinishedUpdated > 0 || pendingUpdated > 0) && cacheManager != null) {
                for (String cacheName : List.of("dashboardSummary", "customerStats", "followupSummaries")) {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerFollowupSummary> findPagedSummaries(
            String timeFilter,
            String statusFilter,
            String search,
            Pageable pageable) {
        return findPagedSummaries(timeFilter, statusFilter, search, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerFollowupSummary> findPagedSummaries(
            String timeFilter,
            String statusFilter,
            String search,
            Instant dateFrom,
            Instant dateTo,
            Pageable pageable) {

        List<TimeFilterPreset> activePresets = timeFilterPresetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        TimeFilterPreset targetPreset = null;
        if (timeFilter != null && !timeFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(timeFilter)) {
            for (TimeFilterPreset p : activePresets) {
                if (String.valueOf(p.getId()).equals(timeFilter.trim())
                        || (p.getPresetKey() != null && p.getPresetKey().equalsIgnoreCase(timeFilter.trim()))
                        || (p.getLabelEn() != null && p.getLabelEn().equalsIgnoreCase(timeFilter.trim()))
                        || (p.getName() != null && p.getName().equalsIgnoreCase(timeFilter.trim()))) {
                    targetPreset = p;
                    break;
                }
            }
        }

        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String status = (statusFilter != null) ? statusFilter.trim().toUpperCase() : "ALL";

        boolean isAlert = "ALERT".equals(status) || "OVERDUE".equals(status) || "NEEDS_ACTION".equals(status) || "ACTION".equals(status);
        boolean isPending = "IDLE".equals(status) || "PENDING".equals(status) || "UPCOMING".equals(status) || "IN_QUEUE".equals(status) || "SCHEDULED".equals(status);
        boolean isCompleted = "DONE".equals(status) || "COMPLETED".equals(status) || "FINISHED".equals(status);

        Page<Customer> customerPage;
        if (targetPreset != null) {
            Long pId = targetPreset.getId();
            String pName = targetPreset.getName() != null ? targetPreset.getName() : targetPreset.getLabelEn();
            if (cleanSearch != null) {
                customerPage = customerRepository.searchCustomersByPreset(pId, pName, cleanSearch, pageable);
            } else if (isAlert) {
                customerPage = customerRepository.findCustomersByPresetAndStatus(pId, pName, "OVERDUE", pageable);
            } else if (isPending) {
                customerPage = customerRepository.findCustomersByPresetAndStatus(pId, pName, "PENDING", pageable);
            } else if (isCompleted) {
                customerPage = customerRepository.findCustomersByPresetAndCompleted(pId, pName, true, pageable);
            } else {
                customerPage = customerRepository.findCustomersByPreset(pId, pName, pageable);
            }
        } else {
            if (cleanSearch != null) {
                customerPage = customerRepository.searchCustomers(cleanSearch, pageable);
            } else if (isAlert) {
                customerPage = customerRepository.findOverdueCustomers(pageable);
            } else if (isPending) {
                customerPage = customerRepository.findInQueueCustomers(pageable);
            } else if (isCompleted) {
                customerPage = customerRepository.findCompletedCustomers(pageable);
            } else if (dateFrom != null && dateTo != null) {
                customerPage = customerRepository.findByCreatedAtBetween(dateFrom, dateTo, pageable);
            } else {
                customerPage = customerRepository.findAll(pageable);
            }
        }

        if (customerPage == null || customerPage.isEmpty()) {
            return new PageImpl<>(
                    Collections.emptyList(), pageable, customerPage != null ? customerPage.getTotalElements() : 0);
        }

        List<Customer> customers = customerPage.getContent();
        List<Long> customerIds = customers.stream().map(Customer::getId).toList();

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

            CustomerFollowupSummary summary = buildSummary(c, orders, checksByOrderId, activePresetIds, activePresetNames, targetPreset, now);
            results.add(summary);
        }

        return new PageImpl<>(results, pageable, customerPage.getTotalElements());
    }

    @Override
    @Cacheable(value = "followupSummaries", key = "#timeFilter != null ? #timeFilter : 'ALL'", sync = true)
    @Transactional(readOnly = true)
    public List<CustomerFollowupSummary> findAllSummaries(String timeFilter) {
        Instant cutoff = DateUtil.getFilterCutoff(timeFilter);
        List<Customer> allCustomers;
        if (cutoff == null) {
            Page<Customer> page = customerRepository.findAll(
                    PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id")));
            if (page != null && page.getContent() != null) {
                allCustomers = page.getContent();
            } else {
                allCustomers = customerRepository.findAll();
            }
        } else {
            List<Customer> paged = customerRepository.findByLastOrderDateAfter(cutoff,
                    PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "lastOrderDate")));
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

            CustomerFollowupSummary summary = buildSummary(c, orders, checksByOrderId, activePresetIds, activePresetNames, null, now);
            results.add(summary);
        }

        return results;
    }

    @Override
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

        boolean isOrderOld = order.getOrderDate() != null
                && Duration.between(order.getOrderDate(), Instant.now()).toDays() >= 120;

        Instant now = Instant.now();
        for (OrderFollowupCheck ch : result) {
            Instant dueTime = calculateDueTime(order.getOrderDate(), ch.getDurationUnit(), ch.getDurationValue());
            if (dueTime != null) {
                int mins = (int) Duration.between(now, dueTime).toMinutes();
                ch.setMinutesUntilDue(mins);
                ch.setIsDue(!ch.isCompleted() && now.isAfter(dueTime));
            }

            boolean hasFeedback = (ch.getNotes() != null && !ch.getNotes().trim().isEmpty())
                    || (ch.getImageUrl() != null && !ch.getImageUrl().trim().isEmpty());

            if (ch.isCompleted() || hasFeedback) {
                if (!ch.isCompleted() || !"COMPLETED".equalsIgnoreCase(ch.getStatus())) {
                    ch.setCompleted(true);
                    ch.setStatus("COMPLETED");
                    followupCheckRepository.save(ch);
                }
            } else if (isOrderOld && !ch.isForcePending()) {
                if (!"UNFINISHED".equalsIgnoreCase(ch.getStatus())) {
                    ch.setStatus("UNFINISHED");
                    ch.setCompleted(false);
                    followupCheckRepository.save(ch);
                }
            } else if ("UNFINISHED".equalsIgnoreCase(ch.getStatus()) || "EXPIRED".equalsIgnoreCase(ch.getStatus())) {
                // Preserve explicitly marked UNFINISHED status!
                ch.setCompleted(false);
            } else {
                // If not in 4 months or if forcePending, it's PENDING (or OVERDUE)
                String expectedStatus = (ch.getIsDue() != null && ch.getIsDue()) ? "OVERDUE" : "PENDING";
                if (!expectedStatus.equalsIgnoreCase(ch.getStatus()) && !"PENDING".equalsIgnoreCase(ch.getStatus())) {
                    ch.setStatus(expectedStatus);
                    followupCheckRepository.save(ch);
                }
            }
        }

        result.sort(Comparator.comparingInt(OrderFollowupCheck::getEquivalentDurationMinutes));
        return result;
    }

    @Override
    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    @Transactional
    public OrderFollowupCheck toggleFollowupStatus(Long checkId) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found with ID: " + checkId));

        if (check.isCompleted()) {
            check.setCompleted(false);
            check.setStatus("PENDING");
            check.setForcePending(true);
            check.setCompletedDate(null);
        } else {
            check.setCompleted(true);
            check.setStatus("COMPLETED");
            check.setCompletedDate(Instant.now());
        }
        return followupCheckRepository.save(check);
    }

    @Override
    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    @Transactional
    public OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl, String satisfaction) {
        return updateFollowupDetails(checkId, notes, photoUrl, satisfaction, null);
    }

    @Override
    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    @Transactional
    public OrderFollowupCheck updateFollowupDetails(Long checkId, String notes, String photoUrl, String satisfaction, String status) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found with ID: " + checkId));

        if (notes != null) check.setNotes(notes);
        if (photoUrl != null) check.setImageUrl(photoUrl);

        if (satisfaction != null && !satisfaction.trim().isEmpty()) {
            check.setSatisfaction(satisfaction.toUpperCase());
        }

        boolean hasFeedback = (check.getNotes() != null && !check.getNotes().trim().isEmpty())
                || (check.getImageUrl() != null && !check.getImageUrl().trim().isEmpty());

        if (status != null && !status.trim().isEmpty()) {
            String upper = status.trim().toUpperCase();
            if ("COMPLETED".equals(upper) || "FINISHED".equals(upper)) {
                check.setStatus("COMPLETED");
                check.setCompleted(true);
                if (check.getCompletedDate() == null) check.setCompletedDate(Instant.now());
            } else if ("UNFINISHED".equals(upper) || "SKIPPED".equals(upper)) {
                check.setStatus("UNFINISHED");
                check.setCompleted(false);
                check.setForcePending(false);
                check.setCompletedDate(null);
            } else {
                check.setStatus("PENDING");
                check.setCompleted(false);
                check.setForcePending(true);
                check.setCompletedDate(null);
            }
        } else {
            if (hasFeedback) {
                check.setStatus("COMPLETED");
                check.setCompleted(true);
                if (check.getSatisfaction() == null || check.getSatisfaction().trim().isEmpty()) {
                    check.setSatisfaction("NEUTRAL");
                }
                if (check.getCompletedDate() == null) check.setCompletedDate(Instant.now());
            } else {
                // Notes/photos deleted/cleared -> transition to PENDING and stay PENDING!
                check.setStatus("PENDING");
                check.setCompleted(false);
                check.setForcePending(true); // "even if u had a unfinshed made a note then delete it will be pending not unfinshed again"
                check.setCompletedDate(null);
                check.setSatisfaction(null);
            }
        }

        Instant now = Instant.now();
        check.setCheckedAt(now);
        check.setUpdatedAt(now);
        if (check.getCheckedBy() == null || check.getCheckedBy().trim().isEmpty()) {
            check.setCheckedBy("Admin");
        }

        return followupCheckRepository.save(check);
    }

    @Override
    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    @Transactional
    public OrderFollowupCheck updateCheckStatus(Long checkId, String status) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found with ID: " + checkId));

        String upper = status != null ? status.trim().toUpperCase() : "PENDING";
        if ("COMPLETED".equals(upper) || "FINISHED".equals(upper)) {
            check.setStatus("COMPLETED");
            check.setCompleted(true);
            check.setCompletedDate(Instant.now());
        } else if ("UNFINISHED".equals(upper) || "SKIPPED".equals(upper)) {
            check.setStatus("UNFINISHED");
            check.setCompleted(false);
            check.setForcePending(false);
            check.setCompletedDate(null);
        } else {
            check.setStatus("PENDING");
            check.setCompleted(false);
            check.setForcePending(true);
            check.setCompletedDate(null);
        }

        check.setUpdatedAt(Instant.now());
        return followupCheckRepository.save(check);
    }

    @Override
    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    @Transactional
    public void markOrderChecksUnfinished(Long orderId) {
        List<OrderFollowupCheck> checks = followupCheckRepository.findByOrderId(orderId);
        Instant now = Instant.now();
        for (OrderFollowupCheck ch : checks) {
            if (!ch.isCompleted()) {
                ch.setStatus("UNFINISHED");
                ch.setCompleted(false);
                ch.setForcePending(false);
                ch.setCompletedDate(null);
                ch.setUpdatedAt(now);
                followupCheckRepository.save(ch);
            }
        }
    }

    @Override
    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    @Transactional
    public void unskipOrderChecks(Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId).orElse(null);
        List<OrderFollowupCheck> checks = followupCheckRepository.findByOrderId(orderId);
        Instant now = Instant.now();
        for (OrderFollowupCheck ch : checks) {
            if (!ch.isCompleted()) {
                ch.setStatus("PENDING");
                ch.setCompleted(false);
                ch.setForcePending(true); // Persist as PENDING so it never flips back to UNFINISHED
                ch.setCompletedDate(null);
                ch.setUpdatedAt(now);
                followupCheckRepository.save(ch);
            }
        }
    }

    @Override
    @CacheEvict(value = {"customerStats", "dashboardSummary", "followupSummaries", "reportData", "satisfactionDetails"}, allEntries = true)
    @Transactional
    public OrderFollowupCheck completeCheck(Long checkId, FollowupCheckRequest req) {
        OrderFollowupCheck check = followupCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up check not found with ID: " + checkId));

        if ("UNFINISHED".equalsIgnoreCase(check.getStatus()) || "EXPIRED".equalsIgnoreCase(check.getStatus())) {
            throw new IllegalStateException("This checkpoint belongs to an unfinished order and cannot receive feedback until restored.");
        }

        check.setCompleted(true);
        check.setCompletedDate(Instant.now());
        check.setCheckedAt(Instant.now());
        check.setStatus("COMPLETED");
        check.setForcePending(false);

        if (req != null) {
            if (req.getNotes() != null) check.setNotes(req.getNotes());
            if (req.getImageUrl() != null) check.setImageUrl(req.getImageUrl());
            if (req.getCheckedBy() != null) check.setCheckedBy(req.getCheckedBy());
            if (req.getSatisfaction() != null) check.setSatisfaction(req.getSatisfaction());
        }

        return followupCheckRepository.save(check);
    }

    private CustomerFollowupSummary buildSummary(Customer c,
                                                 List<CustomerOrder> orders,
                                                 Map<Long, List<OrderFollowupCheck>> checksByOrderId,
                                                 Set<Long> activePresetIds,
                                                 Set<String> activePresetNames,
                                                 TimeFilterPreset targetPreset,
                                                 Instant now) {
        CustomerFollowupSummary summary = new CustomerFollowupSummary();
        summary.setCustomer(c);

        if (orders.isEmpty()) {
            summary.setFollowupStatus("IDLE");
            summary.setNextPendingFollowup("-");
            summary.setAllFollowupsCompleted(false);
            summary.setRemainingFollowupsCount(0);
            return summary;
        }

        // Resolve the customer's latest recorded satisfaction across all checks
        OrderFollowupCheck latestRatedCheck = null;
        Instant latestRatingTime = null;
        for (CustomerOrder ord : orders) {
            List<OrderFollowupCheck> checks = checksByOrderId.getOrDefault(ord.getId(), Collections.emptyList());
            for (OrderFollowupCheck ch : checks) {
                if (ch.getSatisfaction() != null && !ch.getSatisfaction().trim().isEmpty()) {
                    Instant t = ch.getCheckedAt() != null ? ch.getCheckedAt() :
                                (ch.getUpdatedAt() != null ? ch.getUpdatedAt() :
                                (ch.getCompletedDate() != null ? ch.getCompletedDate() :
                                (ord.getOrderDate() != null ? ord.getOrderDate() : ch.getCreatedAt())));
                    if (latestRatedCheck == null || (t != null && (latestRatingTime == null || t.isAfter(latestRatingTime)))) {
                        latestRatedCheck = ch;
                        latestRatingTime = t;
                    }
                }
            }
        }
        if (latestRatedCheck != null) {
            summary.setLatestSatisfaction(latestRatedCheck.getSatisfaction().trim().toUpperCase());
        }

        if (targetPreset != null) {
            OrderFollowupCheck targetCheck = null;
            Long targetOrderId = null;
            CustomerOrder targetOrder = null;

            for (CustomerOrder ord : orders) {
                List<OrderFollowupCheck> checks = checksByOrderId.getOrDefault(ord.getId(), Collections.emptyList());
                for (OrderFollowupCheck ch : checks) {
                    boolean match = (ch.getPreset() != null && targetPreset.getId() != null && targetPreset.getId().equals(ch.getPreset().getId()))
                            || (ch.getMilestoneName() != null && targetPreset.getName() != null && targetPreset.getName().equalsIgnoreCase(ch.getMilestoneName().trim()))
                            || (ch.getMilestoneName() != null && targetPreset.getLabelEn() != null && targetPreset.getLabelEn().equalsIgnoreCase(ch.getMilestoneName().trim()));
                    if (match) {
                        targetCheck = ch;
                        targetOrderId = ord.getId();
                        targetOrder = ord;
                        break;
                    }
                }
                if (targetCheck != null) break;
            }

            if (targetCheck != null) {
                if (targetCheck.getSatisfaction() != null && !targetCheck.getSatisfaction().trim().isEmpty()) {
                    summary.setLatestSatisfaction(targetCheck.getSatisfaction().trim().toUpperCase());
                }
                String mName = targetCheck.getMilestoneName() != null ? targetCheck.getMilestoneName() : targetPreset.getName();
                summary.setNextPendingFollowup(mName);
                summary.setNextPendingOrderId(targetOrderId);

                boolean isOrderOld = targetOrder != null && targetOrder.getOrderDate() != null
                        && Duration.between(targetOrder.getOrderDate(), now).toDays() >= 120;
                boolean isCompleted = targetCheck.isCompleted()
                        || (targetCheck.getNotes() != null && !targetCheck.getNotes().trim().isEmpty())
                        || (targetCheck.getImageUrl() != null && !targetCheck.getImageUrl().trim().isEmpty());
                boolean isTargetUnfinished = !isCompleted && !targetCheck.isForcePending() && (
                        isOrderOld
                        || "UNFINISHED".equalsIgnoreCase(targetCheck.getStatus())
                        || "EXPIRED".equalsIgnoreCase(targetCheck.getStatus())
                        || "SKIPPED".equalsIgnoreCase(targetCheck.getStatus())
                );

                if (isCompleted) {
                    summary.setFollowupStatus("DONE");
                    summary.setAllFollowupsCompleted(true);
                    summary.setRemainingFollowupsCount(0);
                    summary.setNextPendingMinutesLeft(null);
                    return summary;
                } else if (isTargetUnfinished) {
                    summary.setFollowupStatus("UNFINISHED");
                    summary.setAllFollowupsCompleted(false);
                    summary.setRemainingFollowupsCount(0);
                    summary.setNextPendingFollowup("Unfinished");
                    summary.setNextPendingMinutesLeft(null);
                    return summary;
                } else if ("OVERDUE".equalsIgnoreCase(targetCheck.getStatus())) {
                    summary.setFollowupStatus("ALERT");
                    summary.setAllFollowupsCompleted(false);
                    summary.setRemainingFollowupsCount(1);
                    Instant ordDate = targetOrder != null ? targetOrder.getOrderDate() : orders.get(0).getOrderDate();
                    Instant dueTime = calculateDueTime(ordDate, targetCheck.getDurationUnit(), targetCheck.getDurationValue());
                    if (dueTime != null) {
                        summary.setNextPendingMinutesLeft((int) Duration.between(now, dueTime).toMinutes());
                    }
                    return summary;
                } else {
                    summary.setFollowupStatus("IDLE");
                    summary.setAllFollowupsCompleted(false);
                    summary.setRemainingFollowupsCount(1);
                    Instant ordDate = targetOrder != null ? targetOrder.getOrderDate() : orders.get(0).getOrderDate();
                    Instant dueTime = calculateDueTime(ordDate, targetCheck.getDurationUnit(), targetCheck.getDurationValue());
                    if (dueTime != null) {
                        summary.setNextPendingMinutesLeft((int) Duration.between(now, dueTime).toMinutes());
                    }
                    return summary;
                }
            }
        }

        int pendingCount = 0;
        int completedCount = 0;
        int expiredCount = 0;

        OrderFollowupCheck mostOverdueCheck = null;
        Instant earliestOverdueTime = null;
        Long overdueOrderId = null;

        OrderFollowupCheck earliestUpcomingCheck = null;
        Instant earliestUpcomingTime = null;
        Long upcomingOrderId = null;

        for (CustomerOrder ord : orders) {
            boolean isOrderOld = ord.getOrderDate() != null &&
                    Duration.between(ord.getOrderDate(), now).toDays() >= 120; // 4 months
            List<OrderFollowupCheck> checks = checksByOrderId.getOrDefault(ord.getId(), Collections.emptyList());

            for (OrderFollowupCheck ch : checks) {
                boolean isPresetActive = (ch.getPreset() != null && activePresetIds.contains(ch.getPreset().getId()))
                        || (ch.getMilestoneName() != null && activePresetNames.contains(ch.getMilestoneName().trim().toLowerCase()));
                if (!isPresetActive) {
                    continue;
                }

                boolean isCompleted = ch.isCompleted()
                        || (ch.getNotes() != null && !ch.getNotes().trim().isEmpty())
                        || (ch.getImageUrl() != null && !ch.getImageUrl().trim().isEmpty());

                boolean isUnfinished = !isCompleted && !ch.isForcePending() && (
                        isOrderOld
                        || "UNFINISHED".equalsIgnoreCase(ch.getStatus())
                        || "EXPIRED".equalsIgnoreCase(ch.getStatus())
                        || "SKIPPED".equalsIgnoreCase(ch.getStatus())
                );

                if (isCompleted) {
                    completedCount++;
                } else if (isUnfinished) {
                    expiredCount++;
                } else {
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
            if (expiredCount > 0 && completedCount == 0) {
                summary.setFollowupStatus("UNFINISHED");
                summary.setAllFollowupsCompleted(false);
                summary.setNextPendingFollowup("Unfinished");
                summary.setNextPendingOrderId(null);
                summary.setNextPendingMinutesLeft(null);
            } else if (completedCount > 0 && expiredCount == 0) {
                summary.setFollowupStatus("DONE");
                summary.setAllFollowupsCompleted(true);
                summary.setNextPendingFollowup("All Done");
                summary.setNextPendingOrderId(null);
                summary.setNextPendingMinutesLeft(null);
            } else if (expiredCount > 0) {
                summary.setFollowupStatus("UNFINISHED");
                summary.setAllFollowupsCompleted(false);
                summary.setNextPendingFollowup("Unfinished");
                summary.setNextPendingOrderId(null);
                summary.setNextPendingMinutesLeft(null);
            } else {
                summary.setFollowupStatus("IDLE");
                summary.setAllFollowupsCompleted(false);
                summary.setNextPendingFollowup("-");
                summary.setNextPendingOrderId(null);
                summary.setNextPendingMinutesLeft(null);
            }
        } else if (mostOverdueCheck != null) {
            summary.setFollowupStatus("ALERT");
            summary.setAllFollowupsCompleted(false);
            summary.setNextPendingFollowup(mostOverdueCheck.getMilestoneName());
            summary.setNextPendingOrderId(overdueOrderId);
            summary.setNextPendingMinutesLeft((int) Duration.between(now, earliestOverdueTime).toMinutes());
        } else {
            summary.setFollowupStatus("IDLE");
            summary.setAllFollowupsCompleted(false);
            summary.setNextPendingFollowup(earliestUpcomingCheck != null ? earliestUpcomingCheck.getMilestoneName() : "Pending");
            summary.setNextPendingOrderId(upcomingOrderId);
            summary.setNextPendingMinutesLeft(earliestUpcomingTime != null ? (int) Duration.between(now, earliestUpcomingTime).toMinutes() : null);
        }

        return summary;
    }

    private static Instant calculateDueTime(Instant start, String unit, int val) {
        if (start == null) return Instant.now();
        String u = unit != null ? unit.toUpperCase() : "DAYS";
        int v = val > 0 ? val : 1;
        return switch (u) {
            case "SECONDS" -> start.plus(v, ChronoUnit.SECONDS);
            case "HOURS" -> start.plus(v, ChronoUnit.HOURS);
            case "WEEKS" -> start.plus(v * 7L, ChronoUnit.DAYS);
            case "MONTHS" -> start.plus(v * 30L, ChronoUnit.DAYS);
            default -> start.plus(v, ChronoUnit.DAYS);
        };
    }
}
