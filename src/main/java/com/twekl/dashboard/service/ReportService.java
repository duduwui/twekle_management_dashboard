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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;

    @Autowired
    public ReportService(CustomerRepository customerRepository,
                         CustomerOrderRepository orderRepository,
                         OrderFollowupCheckRepository followupCheckRepository,
                         CustomerFeedbackRepository feedbackRepository,
                         TimeFilterPresetRepository timeFilterPresetRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.feedbackRepository = feedbackRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
    }

    public Map<String, Object> generateFollowupReport(String period, String dateFrom, String dateTo) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime = LocalDateTime.now();

        LocalDate today = LocalDate.now();

        if (dateFrom != null && !dateFrom.trim().isEmpty() && dateTo != null && !dateTo.trim().isEmpty()) {
            try {
                startDateTime = LocalDate.parse(dateFrom.trim()).atStartOfDay();
                endDateTime = LocalDate.parse(dateTo.trim()).atTime(LocalTime.MAX);
            } catch (Exception e) {
                startDateTime = today.minusDays(30).atStartOfDay();
            }
        } else if ("daily".equalsIgnoreCase(period)) {
            startDateTime = today.atStartOfDay();
            endDateTime = today.atTime(LocalTime.MAX);
        } else if ("weekly".equalsIgnoreCase(period)) {
            startDateTime = today.minusDays(7).atStartOfDay();
        } else if ("monthly".equalsIgnoreCase(period)) {
            startDateTime = today.minusDays(30).atStartOfDay();
        } else {
            // Default: All Time
            startDateTime = LocalDateTime.of(2020, 1, 1, 0, 0);
        }

        // Fetch all customers, orders, and checks
        List<Customer> allCustomers = customerRepository.findAll();
        Map<Long, Customer> customerMap = allCustomers.stream().collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));

        List<CustomerOrder> allOrders = orderRepository.findAll();
        
        // Filter orders within the selected time period
        final LocalDateTime finalStart = startDateTime;
        final LocalDateTime finalEnd = endDateTime;

        List<CustomerOrder> filteredOrders = allOrders.stream()
                .filter(o -> {
                    LocalDateTime dt = o.getOrderDate() != null ? o.getOrderDate() : o.getCreatedAt();
                    if (dt == null) return true;
                    return !dt.isBefore(finalStart) && !dt.isAfter(finalEnd);
                })
                .sorted(Comparator.comparing((CustomerOrder o) -> o.getOrderDate() != null ? o.getOrderDate() : LocalDateTime.MIN).reversed())
                .collect(Collectors.toList());

        Set<Long> filteredOrderIds = filteredOrders.stream().map(CustomerOrder::getId).collect(Collectors.toSet());

        // Gather all follow-up checks for the filtered orders
        List<OrderFollowupCheck> checksInPeriod = new ArrayList<>();
        Map<Long, List<OrderFollowupCheck>> orderChecksMap = new HashMap<>();

        for (CustomerOrder order : filteredOrders) {
            List<OrderFollowupCheck> checks = followupCheckRepository.findByOrderIdOrderByIdAsc(order.getId());
            orderChecksMap.put(order.getId(), checks);
            checksInPeriod.addAll(checks);
        }

        // 1. Overall Summary Metrics
        int totalChecks = checksInPeriod.size();
        long completedChecks = checksInPeriod.stream().filter(c -> Boolean.TRUE.equals(c.getIsCompleted())).count();
        long pendingChecks = totalChecks - completedChecks;
        double completionRate = totalChecks > 0 ? Math.round((completedChecks * 1000.0) / totalChecks) / 10.0 : 0.0;

        // Satisfaction Counts from checks and feedbacks
        long satisfiedCount = 0;
        long neutralCount = 0;
        long unsatisfiedCount = 0;
        long blankNotesCount = 0;
        int totalNotesWithContent = 0;

        for (OrderFollowupCheck chk : checksInPeriod) {
            String sat = chk.getSatisfaction();
            boolean hasNote = (chk.getNote() != null && !chk.getNote().trim().isEmpty()) 
                    || (chk.getImageUrl() != null && !chk.getImageUrl().trim().isEmpty());
            
            if (hasNote || sat != null) {
                totalNotesWithContent++;
                if ("SATISFIED".equalsIgnoreCase(sat)) {
                    satisfiedCount++;
                } else if ("NEUTRAL".equalsIgnoreCase(sat)) {
                    neutralCount++;
                } else if ("UNSATISFIED".equalsIgnoreCase(sat)) {
                    unsatisfiedCount++;
                } else {
                    blankNotesCount++;
                }
            }
        }

        // Also incorporate feedbacks in period
        List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();
        List<CustomerFeedback> feedbacksInPeriod = allFeedbacks.stream()
                .filter(fb -> {
                    LocalDateTime dt = fb.getCreatedAt();
                    if (dt == null) return true;
                    return !dt.isBefore(finalStart) && !dt.isAfter(finalEnd);
                })
                .collect(Collectors.toList());

        for (CustomerFeedback fb : feedbacksInPeriod) {
            // If already counted via order checks, avoid duplicating if same ID, or add feedback sentiment
            String sat = fb.getSatisfaction();
            if (sat != null) {
                if ("SATISFIED".equalsIgnoreCase(sat)) satisfiedCount++;
                else if ("NEUTRAL".equalsIgnoreCase(sat)) neutralCount++;
                else if ("UNSATISFIED".equalsIgnoreCase(sat)) unsatisfiedCount++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalFollowupsDue", totalChecks);
        summary.put("finishedFollowups", completedChecks);
        summary.put("pendingFollowups", pendingChecks);
        summary.put("completionRate", completionRate);
        summary.put("totalOrdersInPeriod", filteredOrders.size());
        summary.put("totalCustomersInPeriod", filteredOrders.stream().map(CustomerOrder::getCustomerId).distinct().count());
        
        Map<String, Object> satisfactionSummary = new LinkedHashMap<>();
        satisfactionSummary.put("satisfied", satisfiedCount);
        satisfactionSummary.put("neutral", neutralCount);
        satisfactionSummary.put("unsatisfied", unsatisfiedCount);
        satisfactionSummary.put("blank", blankNotesCount);
        satisfactionSummary.put("totalNotes", totalNotesWithContent);
        summary.put("satisfaction", satisfactionSummary);

        // 2. Milestone Performance Breakdown
        List<TimeFilterPreset> presets = timeFilterPresetRepository.findAll();
        Map<String, int[]> presetCounts = new LinkedHashMap<>(); // [total, completed]

        for (TimeFilterPreset p : presets) {
            presetCounts.put(p.getName(), new int[]{0, 0});
        }

        for (OrderFollowupCheck c : checksInPeriod) {
            String name = c.getPresetName();
            if (name != null) {
                presetCounts.putIfAbsent(name, new int[]{0, 0});
                presetCounts.get(name)[0]++;
                if (Boolean.TRUE.equals(c.getIsCompleted())) {
                    presetCounts.get(name)[1]++;
                }
            }
        }

        List<Map<String, Object>> milestoneList = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : presetCounts.entrySet()) {
            int tot = entry.getValue()[0];
            int comp = entry.getValue()[1];
            int pend = tot - comp;
            double pct = tot > 0 ? Math.round((comp * 1000.0) / tot) / 10.0 : 0.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("presetName", entry.getKey());
            item.put("total", tot);
            item.put("completed", comp);
            item.put("pending", pend);
            item.put("completionRate", pct);
            milestoneList.add(item);
        }

        // 3. Pending Follow-ups List (Needing Contact)
        List<Map<String, Object>> pendingList = new ArrayList<>();
        for (CustomerOrder order : filteredOrders) {
            List<OrderFollowupCheck> checks = orderChecksMap.getOrDefault(order.getId(), Collections.emptyList());
            Customer cust = customerMap.get(order.getCustomerId());

            for (OrderFollowupCheck c : checks) {
                if (!Boolean.TRUE.equals(c.getIsCompleted())) {
                    Map<String, Object> pItem = new LinkedHashMap<>();
                    pItem.put("checkId", c.getId());
                    pItem.put("orderId", order.getId());
                    pItem.put("orderNumber", order.getOrderNumber());
                    pItem.put("customerId", order.getCustomerId());
                    pItem.put("customerName", cust != null ? cust.getName() : "Unknown");
                    pItem.put("customerPhone", cust != null ? cust.getPhoneNumber() : "-");
                    pItem.put("customerCity", cust != null ? cust.getCity() : "Erbil");
                    pItem.put("presetName", c.getPresetName());
                    pItem.put("orderDate", order.getOrderDate());
                    pItem.put("itemsSummary", order.getItemsSummary() != null ? order.getItemsSummary() : "-");
                    long days = order.getOrderDate() != null ? ChronoUnit.DAYS.between(order.getOrderDate().toLocalDate(), today) : 0;
                    pItem.put("daysSinceOrder", days);
                    pendingList.add(pItem);
                }
            }
        }

        // 4. Detailed Notes & Satisfaction Ledger
        List<Map<String, Object>> notesLedger = new ArrayList<>();
        for (CustomerOrder order : filteredOrders) {
            List<OrderFollowupCheck> checks = orderChecksMap.getOrDefault(order.getId(), Collections.emptyList());
            Customer cust = customerMap.get(order.getCustomerId());

            for (OrderFollowupCheck c : checks) {
                boolean hasContent = (c.getNote() != null && !c.getNote().trim().isEmpty()) 
                        || (c.getImageUrl() != null && !c.getImageUrl().trim().isEmpty())
                        || (c.getSatisfaction() != null && !c.getSatisfaction().trim().isEmpty());

                if (hasContent) {
                    Map<String, Object> nItem = new LinkedHashMap<>();
                    nItem.put("id", c.getId());
                    nItem.put("orderId", order.getId());
                    nItem.put("orderNumber", order.getOrderNumber());
                    nItem.put("customerId", order.getCustomerId());
                    nItem.put("customerName", cust != null ? cust.getName() : "Unknown");
                    nItem.put("customerPhone", cust != null ? cust.getPhoneNumber() : "-");
                    nItem.put("presetName", c.getPresetName());
                    nItem.put("itemsSummary", order.getItemsSummary() != null ? order.getItemsSummary() : "-");
                    nItem.put("satisfaction", c.getSatisfaction() != null ? c.getSatisfaction() : "BLANK");
                    nItem.put("note", c.getNote() != null ? c.getNote() : "");
                    nItem.put("hasImage", c.getImageUrl() != null && !c.getImageUrl().trim().isEmpty());
                    nItem.put("imageUrl", c.getImageUrl());
                    nItem.put("checkedBy", c.getCheckedBy() != null ? c.getCheckedBy() : "Follow-up Agent");
                    nItem.put("date", c.getCheckedAt() != null ? c.getCheckedAt() : (c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt()));
                    notesLedger.add(nItem);
                }
            }
        }

        // Sort notes descending by date
        notesLedger.sort((a, b) -> {
            LocalDateTime da = (LocalDateTime) a.get("date");
            LocalDateTime db = (LocalDateTime) b.get("date");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        // 5. Product Insights & Satisfaction Breakdown
        Map<String, Map<String, Object>> productMap = new LinkedHashMap<>();

        for (CustomerOrder order : filteredOrders) {
            String items = order.getItemsSummary();
            if (items == null || items.trim().isEmpty()) continue;

            // Split items if comma separated
            String[] tokens = items.split("[,;\\n]+");
            for (String t : tokens) {
                String prodName = t.trim();
                if (prodName.isEmpty()) continue;

                productMap.putIfAbsent(prodName, new LinkedHashMap<>());
                Map<String, Object> pData = productMap.get(prodName);
                pData.putIfAbsent("productName", prodName);
                pData.put("orderCount", (int) pData.getOrDefault("orderCount", 0) + 1);
                pData.putIfAbsent("satisfiedCount", 0);
                pData.putIfAbsent("neutralCount", 0);
                pData.putIfAbsent("unsatisfiedCount", 0);
                pData.putIfAbsent("notesCount", 0);

                // Check if checks on this order have notes/satisfaction
                List<OrderFollowupCheck> checks = orderChecksMap.getOrDefault(order.getId(), Collections.emptyList());
                for (OrderFollowupCheck chk : checks) {
                    if (chk.getSatisfaction() != null) {
                        String sat = chk.getSatisfaction();
                        if ("SATISFIED".equalsIgnoreCase(sat)) {
                            pData.put("satisfiedCount", (int) pData.get("satisfiedCount") + 1);
                        } else if ("NEUTRAL".equalsIgnoreCase(sat)) {
                            pData.put("neutralCount", (int) pData.get("neutralCount") + 1);
                        } else if ("UNSATISFIED".equalsIgnoreCase(sat)) {
                            pData.put("unsatisfiedCount", (int) pData.get("unsatisfiedCount") + 1);
                        }
                    }
                    if (chk.getNote() != null && !chk.getNote().trim().isEmpty()) {
                        pData.put("notesCount", (int) pData.get("notesCount") + 1);
                    }
                }
            }
        }

        List<Map<String, Object>> productList = new ArrayList<>(productMap.values());
        productList.sort(Comparator.comparing((Map<String, Object> p) -> (int) p.get("orderCount")).reversed());

        // 6. Customer Summary in Period
        Map<Long, Map<String, Object>> customerSummaryMap = new LinkedHashMap<>();
        for (CustomerOrder order : filteredOrders) {
            Long cId = order.getCustomerId();
            Customer cust = customerMap.get(cId);
            if (cust == null) continue;

            customerSummaryMap.putIfAbsent(cId, new LinkedHashMap<>());
            Map<String, Object> cData = customerSummaryMap.get(cId);
            cData.put("id", cust.getId());
            cData.put("name", cust.getName());
            cData.put("phoneNumber", cust.getPhoneNumber());
            cData.put("city", cust.getCity() != null ? cust.getCity() : "Erbil");
            cData.put("totalOrders", (int) cData.getOrDefault("totalOrders", 0) + 1);
            double amt = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;
            cData.put("totalSpent", (double) cData.getOrDefault("totalSpent", 0.0) + amt);

            List<OrderFollowupCheck> checks = orderChecksMap.getOrDefault(order.getId(), Collections.emptyList());
            long pendingInOrder = checks.stream().filter(c -> !Boolean.TRUE.equals(c.getIsCompleted())).count();
            cData.put("pendingFollowups", (int) cData.getOrDefault("pendingFollowups", 0) + (int) pendingInOrder);
        }

        List<Map<String, Object>> customerSummaryList = new ArrayList<>(customerSummaryMap.values());
        for (Map<String, Object> c : customerSummaryList) {
            int p = (int) c.get("pendingFollowups");
            c.put("allFollowupsCompleted", p == 0);
        }
        customerSummaryList.sort(Comparator.comparing((Map<String, Object> c) -> (double) c.get("totalSpent")).reversed());

        // Final payload
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", period);
        report.put("startDate", startDateTime);
        report.put("endDate", endDateTime);
        report.put("summary", summary);
        report.put("milestones", milestoneList);
        report.put("pendingFollowups", pendingList);
        report.put("satisfactionNotes", notesLedger);
        report.put("products", productList);
        report.put("customerSummary", customerSummaryList);

        return report;
    }
}
