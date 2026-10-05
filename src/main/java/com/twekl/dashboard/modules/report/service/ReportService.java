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
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final com.twekl.dashboard.modules.timefilter.repository.TimeFilterPresetRepository presetRepository;

    public ReportService(CustomerRepository customerRepository,
                         CustomerOrderRepository orderRepository,
                         CustomerFeedbackRepository feedbackRepository,
                         OrderFollowupCheckRepository followupCheckRepository,
                         com.twekl.dashboard.modules.timefilter.repository.TimeFilterPresetRepository presetRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.presetRepository = presetRepository;
    }

    public Map<String, Object> generateReport(String period) {
        LocalDateTime cutoff = getCutoffForPeriod(period);

        List<Customer>          allCustomers = customerRepository.findAll();
        List<CustomerOrder>     allOrders    = orderRepository.findAll();
        List<CustomerFeedback>  allFeedbacks = feedbackRepository.findAll();
        List<OrderFollowupCheck> allChecks   = followupCheckRepository.findAll();
        List<com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset> activePresets = presetRepository.findByActiveTrueOrderByDisplayOrderAsc();

        // ── Summary numbers ──────────────────────────────────────────
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
                .filter(c -> "COMPLETED".equalsIgnoreCase(c.getStatus()) || c.getIsCompleted())
                .count();

        double completionRate = allChecks.isEmpty() ? 100.0
                : ((double) completedChecks / allChecks.size()) * 100.0;

        double avgRating = allFeedbacks.stream()
                .mapToInt(CustomerFeedback::getRating)
                .average()
                .orElse(5.0);

        // ── milestones: strictly synced with configured TimeFilterPresets ──
        List<Map<String, Object>> milestones = new ArrayList<>();
        for (com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset preset : activePresets) {
            long total = 0;
            long done  = 0;
            for (OrderFollowupCheck ch : allChecks) {
                boolean matches = (ch.getPresetId() != null && ch.getPresetId().equals(preset.getId()))
                        || (ch.getPresetName() != null && ch.getPresetName().equalsIgnoreCase(preset.getLabelEn()))
                        || (preset.getPresetKey() != null && ch.getPresetName() != null && ch.getPresetName().equalsIgnoreCase(preset.getPresetKey()));
                if (matches) {
                    total++;
                    if ("COMPLETED".equalsIgnoreCase(ch.getStatus()) || ch.getIsCompleted()) {
                        done++;
                    }
                }
            }
            long pending = total - done;
            double rate = total == 0 ? 100.0 : Math.round((done * 100.0 / total) * 10.0) / 10.0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("presetName",     preset.getLabelEn());
            row.put("total",          total);
            row.put("completed",      done);
            row.put("pending",        pending);
            row.put("completionRate", rate);
            milestones.add(row);
        }


        // ── satisfaction: build recentNotes list from feedbacks ──────
        List<Map<String, Object>> recentNotes = new ArrayList<>();
        for (CustomerFeedback fb : allFeedbacks) {
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("customerName",  fb.getCustomer() != null ? fb.getCustomer().getName()        : "");
            note.put("customerPhone", fb.getCustomer() != null ? fb.getCustomer().getPhoneNumber() : "");
            note.put("presetName",    fb.getMilestoneLabel() != null ? fb.getMilestoneLabel() : "");
            // Derive satisfaction label from rating
            int rating = fb.getRating();
            String sat = rating >= 4 ? "SATISFIED" : (rating >= 3 ? "NEUTRAL" : "UNSATISFIED");
            note.put("satisfaction",  sat);
            note.put("note",          fb.getContent() != null ? fb.getContent() : "");
            note.put("imageUrl",      fb.getPhotoUrl() != null ? fb.getPhotoUrl() : "");
            note.put("hasImage",      fb.getPhotoUrl() != null && !fb.getPhotoUrl().isEmpty());
            note.put("date",          fb.getCreatedAt());
            recentNotes.add(note);
        }
        Map<String, Object> satisfaction = new LinkedHashMap<>();
        satisfaction.put("breakdown",   Collections.emptyList());
        satisfaction.put("recentNotes", recentNotes);

        // ── products: group orders by productName ────────────────────
        Map<String, Map<String, Object>> productMap = new LinkedHashMap<>();
        for (CustomerOrder o : allOrders) {
            String pName = o.getProductName() != null ? o.getProductName() : "Unknown";
            productMap.computeIfAbsent(pName, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("productName",      k);
                m.put("orderCount",       0L);
                m.put("satisfiedCount",   0L);
                m.put("neutralCount",     0L);
                m.put("unsatisfiedCount", 0L);
                m.put("notesCount",       0L);
                return m;
            });
            Map<String, Object> pm = productMap.get(pName);
            pm.put("orderCount", (Long) pm.get("orderCount") + 1L);
        }
        // Count feedbacks per product
        for (CustomerFeedback fb : allFeedbacks) {
            String pName = "Unknown";
            if (fb.getOrderId() != null) {
                Optional<CustomerOrder> ord = orderRepository.findById(fb.getOrderId());
                if (ord.isPresent() && ord.get().getProductName() != null) pName = ord.get().getProductName();
            }
            if (productMap.containsKey(pName)) {
                Map<String, Object> pm = productMap.get(pName);
                int rating = fb.getRating();
                if (rating >= 4)      pm.put("satisfiedCount",   (Long)pm.get("satisfiedCount")   + 1L);
                else if (rating >= 3) pm.put("neutralCount",     (Long)pm.get("neutralCount")     + 1L);
                else                  pm.put("unsatisfiedCount", (Long)pm.get("unsatisfiedCount") + 1L);
                if (fb.getContent() != null && !fb.getContent().isEmpty())
                    pm.put("notesCount", (Long)pm.get("notesCount") + 1L);
            }
        }
        List<Map<String, Object>> products = new ArrayList<>(productMap.values());

        // ── customers: customer directory list ───────────────────────
        List<Map<String, Object>> customers = new ArrayList<>();
        for (Customer c : allCustomers) {
            long orders  = allOrders.stream().filter(o -> o.getCustomer() != null && c.getId().equals(o.getCustomer().getId())).count();
            double spent = allOrders.stream().filter(o -> o.getCustomer() != null && c.getId().equals(o.getCustomer().getId())).mapToDouble(CustomerOrder::getTotalPrice).sum();
            long pending = allChecks.stream().filter(ch -> {
                if (ch.getOrder() == null || ch.getOrder().getCustomer() == null) return false;
                return c.getId().equals(ch.getOrder().getCustomer().getId()) && !"COMPLETED".equalsIgnoreCase(ch.getStatus());
            }).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",              c.getId());
            row.put("name",            c.getName());
            row.put("phoneNumber",     c.getPhoneNumber());
            row.put("city",            c.getCity());
            row.put("totalOrders",     orders);
            row.put("totalSpent",      Math.round(spent * 100.0) / 100.0);
            row.put("pendingFollowups",pending);
            row.put("createdAt",       c.getCreatedAt());
            customers.add(row);
        }

        // ── assemble final response ──────────────────────────────────
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period",          period != null ? period : "monthly");
        report.put("newCustomers",    newCustomers);
        report.put("totalOrders",     totalOrdersInPeriod);
        report.put("totalRevenue",    Math.round(revenueInPeriod * 100.0) / 100.0);
        report.put("completionRate",  Math.round(completionRate * 10.0) / 10.0);
        report.put("averageRating",   Math.round(avgRating * 10.0) / 10.0);
        report.put("totalFeedbacks",  allFeedbacks.size());
        // Keys the frontend expects:
        report.put("milestones",      milestones);
        report.put("satisfaction",    satisfaction);
        report.put("products",        products);
        report.put("customers",       customers);
        return report;
    }

    private LocalDateTime getCutoffForPeriod(String period) {
        if (period == null) return LocalDateTime.now().minusDays(30);
        String p = period.toLowerCase();
        if (p.contains("day") || p.contains("daily") || p.contains("24h")) return LocalDateTime.now().minusHours(24);
        if (p.contains("week"))  return LocalDateTime.now().minusDays(7);
        if (p.contains("month")) return LocalDateTime.now().minusDays(30);
        if (p.contains("year"))  return LocalDateTime.now().minusYears(1);
        if (p.contains("all"))   return null;
        return LocalDateTime.now().minusDays(30);
    }
}
