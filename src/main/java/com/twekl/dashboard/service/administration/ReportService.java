package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.SatisfactionReportDto;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.repository.administration.CustomerRepository;
import com.twekl.dashboard.repository.administration.CustomerFeedbackRepository;
import com.twekl.dashboard.repository.administration.CustomerOrderRepository;
import com.twekl.dashboard.repository.administration.OrderFollowupCheckRepository;
import com.twekl.dashboard.repository.administration.TimeFilterPresetRepository;
import com.twekl.dashboard.util.administration.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
public class ReportService {

    private final CustomerRepository customerRepository;

    private final CustomerOrderRepository orderRepository;

    private final CustomerFeedbackRepository feedbackRepository;

    private final OrderFollowupCheckRepository followupCheckRepository;

    private final TimeFilterPresetRepository presetRepository;

    public ReportService(CustomerRepository customerRepository,
                          CustomerOrderRepository orderRepository,
                          CustomerFeedbackRepository feedbackRepository,
                          OrderFollowupCheckRepository followupCheckRepository,
                          TimeFilterPresetRepository presetRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.presetRepository = presetRepository;
    }

    public Map<String, Object> generateReport(String period) {
        Instant cutoff = getCutoffForPeriod(period);

        List<Customer> allCustomers = customerRepository.findAll();
        List<CustomerOrder> allOrders = orderRepository.findAllWithCustomerAndChecks();
        List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();
        List<OrderFollowupCheck> allChecks = followupCheckRepository.findAllWithOrderCustomerAndPreset();
        List<TimeFilterPreset> activePresets = presetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        java.util.Set<Long> activePresetIds = new java.util.HashSet<>();
        java.util.Set<String> activePresetNames = new java.util.HashSet<>();
        for (TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getName() != null) activePresetNames.add(p.getName().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        // 1. High-level Summary Metrics
        long newCustomers = allCustomers.stream()
                .filter(c -> cutoff == null || (c.getCreatedAt() != null && c.getCreatedAt().isAfter(cutoff)))
                .count();

        List<CustomerOrder> filteredOrders = allOrders.stream()
                .filter(o -> cutoff == null || (o.getOrderDate() != null && o.getOrderDate().isAfter(cutoff)))
                .collect(Collectors.toList());

        long totalOrdersInPeriod = filteredOrders.size();

        double revenueInPeriod = filteredOrders.stream()
                .mapToDouble(CustomerOrder::getTotalAmount)
                .sum();

        List<OrderFollowupCheck> checksInPeriod = allChecks.stream()
                .filter(c -> {
                    boolean isPresetActive = (c.getPreset() != null && activePresetIds.contains(c.getPreset().getId()))
                            || (c.getMilestoneName() != null && activePresetNames.contains(c.getMilestoneName().trim().toLowerCase()));
                    if (!isPresetActive) return false;

                    if (cutoff == null) return true;
                    Instant checkDate = c.getCheckedAt() != null ? c.getCheckedAt() :
                                        (c.getUpdatedAt() != null ? c.getUpdatedAt() :
                                        (c.getCompletedDate() != null ? c.getCompletedDate() :
                                        (c.getOrder() != null ? c.getOrder().getOrderDate() : null)));
                    return checkDate != null && checkDate.isAfter(cutoff);
                })
                .collect(Collectors.toList());

        long completedChecks = checksInPeriod.stream()
                .filter(OrderFollowupCheck::isCompleted)
                .count();

        long totalChecks = checksInPeriod.size();
        double completionRate = totalChecks > 0 ? (double) completedChecks / totalChecks * 100.0 : 0.0;

        double avgRating = allFeedbacks.stream()
                .filter(f -> cutoff == null || (f.getFeedbackDate() != null && f.getFeedbackDate().isAfter(cutoff)))
                .mapToInt(CustomerFeedback::getRating)
                .average()
                .orElse(0.0);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("newCustomers", newCustomers);
        summary.put("totalOrders", totalOrdersInPeriod);
        summary.put("revenue", Math.round(revenueInPeriod * 100.0) / 100.0);
        summary.put("completedFollowups", completedChecks);
        summary.put("completionRate", Math.round(completionRate * 10.0) / 10.0);
        summary.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        summary.put("totalFeedbacks", allFeedbacks.size());

        // 2. Milestones Matrix (Health by Active Preset)
        activePresets.sort(Comparator.comparingInt(TimeFilterPreset::getEquivalentDurationMinutes));
        Map<String, int[]> presetCounts = new LinkedHashMap<>();
        for (TimeFilterPreset p : activePresets) {
            presetCounts.put(p.getName(), new int[]{0, 0});
        }

        for (OrderFollowupCheck c : checksInPeriod) {
            String name = c.getMilestoneName();
            if (name != null && !name.trim().isEmpty() && presetCounts.containsKey(name.trim())) {
                presetCounts.get(name.trim())[0]++;
                if (c.isCompleted()) {
                    presetCounts.get(name.trim())[1]++;
                }
            }
        }

        List<Map<String, Object>> milestoneList = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : presetCounts.entrySet()) {
            int tot = entry.getValue()[0];
            int comp = entry.getValue()[1];
            int pend = tot - comp;
            double pct = tot > 0 ? Math.round((comp * 1000.0) / tot) / 10.0 : 0.0;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("presetName", entry.getKey());
            m.put("total", tot);
            m.put("completed", comp);
            m.put("pending", pend);
            m.put("completionRate", pct);
            milestoneList.add(m);
        }

        // 3. Customer Satisfaction & Notes Ledger
        List<Map<String, Object>> notesLedger = new ArrayList<>();
        for (OrderFollowupCheck c : checksInPeriod) {
            boolean hasContent = (c.getNotes() != null && !c.getNotes().trim().isEmpty())
                    || (c.getImageUrl() != null && !c.getImageUrl().trim().isEmpty())
                    || (c.getSatisfaction() != null && !c.getSatisfaction().trim().isEmpty());

            if (hasContent) {
                CustomerOrder ord = c.getOrder();
                Customer cust = ord != null ? ord.getCustomer() : null;

                Map<String, Object> nItem = new LinkedHashMap<>();
                nItem.put("id", c.getId());
                nItem.put("orderId", ord != null ? ord.getId() : null);
                nItem.put("orderNumber", ord != null ? ord.getOrderNumber() : "-");
                nItem.put("customerId", cust != null ? cust.getId() : null);
                nItem.put("customerName", cust != null ? cust.getName() : "Unknown");
                nItem.put("customerPhone", cust != null ? cust.getPhoneNumber() : "-");
                nItem.put("customerCity", cust != null ? cust.getCity() : "Erbil");
                nItem.put("presetName", c.getMilestoneName() != null ? c.getMilestoneName() : "Follow-up");
                nItem.put("itemsSummary", ord != null && ord.getItemsSummary() != null ? ord.getItemsSummary() : "-");
                String sat = c.getSatisfaction() != null && !c.getSatisfaction().trim().isEmpty()
                        ? c.getSatisfaction().toUpperCase()
                        : "NEUTRAL";
                nItem.put("satisfaction", sat);
                nItem.put("note", c.getNotes() != null ? c.getNotes() : "");
                nItem.put("hasImage", c.getImageUrl() != null && !c.getImageUrl().trim().isEmpty());
                nItem.put("imageUrl", c.getImageUrl());
                nItem.put("checkedBy", c.getCheckedBy() != null ? c.getCheckedBy() : "Agent");
                Instant itemDate = c.getCheckedAt() != null ? c.getCheckedAt() :
                                   (c.getUpdatedAt() != null ? c.getUpdatedAt() :
                                   (c.getCompletedDate() != null ? c.getCompletedDate() :
                                   (ord != null && ord.getOrderDate() != null ? ord.getOrderDate() : Instant.now())));
                nItem.put("date", itemDate);
                notesLedger.add(nItem);
            }
        }

        notesLedger.sort((a, b) -> {
            Instant da = (Instant) a.get("date");
            Instant db = (Instant) b.get("date");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        long satCount = 0;
        long neuCount = 0;
        long unsatCount = 0;
        for (Map<String, Object> item : notesLedger) {
            String s = ((String) item.get("satisfaction")).toUpperCase();
            if ("SATISFIED".equals(s) || "4".equals(s) || "5".equals(s)) satCount++;
            else if ("UNSATISFIED".equals(s) || "1".equals(s) || "2".equals(s)) unsatCount++;
            else neuCount++;
        }
        long totalRated = satCount + neuCount + unsatCount;
        double positiveRate = totalRated > 0 ? Math.round(((double) satCount / totalRated * 100.0) * 10.0) / 10.0 : 100.0;

        Map<String, Object> satisfactionMap = new LinkedHashMap<>();
        satisfactionMap.put("recentNotes", notesLedger);
        satisfactionMap.put("totalRated", totalRated);
        satisfactionMap.put("satisfiedCount", satCount);
        satisfactionMap.put("neutralCount", neuCount);
        satisfactionMap.put("unsatisfiedCount", unsatCount);
        satisfactionMap.put("positiveRate", positiveRate);

        summary.put("satisfactionRate", positiveRate);
        summary.put("satisfiedCount", satCount);
        summary.put("neutralCount", neuCount);
        summary.put("unsatisfiedCount", unsatCount);
        summary.put("totalRated", totalRated);

        // 4. Product Insights & Reviews
        Map<String, Map<String, Object>> productMap = new LinkedHashMap<>();
        for (CustomerOrder ord : filteredOrders) {
            String items = ord.getItemsSummary();
            if (items == null || items.trim().isEmpty()) continue;

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

                Collection<OrderFollowupCheck> orderChecks = ord.getChecks();
                if (orderChecks != null) {
                    for (OrderFollowupCheck chk : orderChecks) {
                        if (chk.getSatisfaction() != null) {
                            String sat = chk.getSatisfaction().toUpperCase();
                            if ("SATISFIED".equals(sat) || "5".equals(sat) || "4".equals(sat)) {
                                pData.put("satisfiedCount", (int) pData.get("satisfiedCount") + 1);
                            } else if ("NEUTRAL".equals(sat) || "3".equals(sat)) {
                                pData.put("neutralCount", (int) pData.get("neutralCount") + 1);
                            } else if ("UNSATISFIED".equals(sat) || "1".equals(sat) || "2".equals(sat)) {
                                pData.put("unsatisfiedCount", (int) pData.get("unsatisfiedCount") + 1);
                            }
                        }
                        if (chk.getNotes() != null && !chk.getNotes().trim().isEmpty()) {
                            pData.put("notesCount", (int) pData.get("notesCount") + 1);
                        }
                    }
                }
            }
        }
        List<Map<String, Object>> productList = new ArrayList<>(productMap.values());
        productList.sort(Comparator.comparing((Map<String, Object> p) -> (int) p.get("orderCount")).reversed());

        // 5. Customer Directory Summary
        Map<Long, Map<String, Object>> customerSummaryMap = new LinkedHashMap<>();
        for (Customer cust : allCustomers) {
            List<CustomerOrder> custOrders = filteredOrders.stream()
                    .filter(o -> o.getCustomer() != null && o.getCustomer().getId().equals(cust.getId()))
                    .collect(Collectors.toList());

            if (cutoff != null && custOrders.isEmpty()) continue;

            Map<String, Object> cData = new LinkedHashMap<>();
            cData.put("id", cust.getId());
            cData.put("name", cust.getName());
            cData.put("phoneNumber", cust.getPhoneNumber());
            cData.put("city", cust.getCity() != null ? cust.getCity() : "Erbil");
            cData.put("totalOrders", custOrders.size());
            double totalSpent = custOrders.stream().mapToDouble(CustomerOrder::getTotalAmount).sum();
            cData.put("totalSpent", Math.round(totalSpent * 100.0) / 100.0);

            int pendingFollowups = 0;
            for (CustomerOrder o : custOrders) {
                if (o.getChecks() != null) {
                    for (OrderFollowupCheck chk : o.getChecks()) {
                        boolean isPresetActive = (chk.getPreset() != null && activePresetIds.contains(chk.getPreset().getId()))
                                || (chk.getMilestoneName() != null && activePresetNames.contains(chk.getMilestoneName().trim().toLowerCase()));
                        if (isPresetActive && !chk.isCompleted()) {
                            pendingFollowups++;
                        }
                    }
                }
            }
            cData.put("pendingFollowups", pendingFollowups);
            cData.put("allFollowupsCompleted", pendingFollowups == 0);
            customerSummaryMap.put(cust.getId(), cData);
        }
        List<Map<String, Object>> customerSummaryList = new ArrayList<>(customerSummaryMap.values());
        customerSummaryList.sort(Comparator.comparing((Map<String, Object> c) -> (double) c.get("totalSpent")).reversed());

        // Assembly
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", period);
        report.put("generatedAt", Instant.now());
        report.put("summary", summary);
        report.put("milestones", milestoneList);
        report.put("satisfaction", satisfactionMap);
        report.put("satisfactionNotes", notesLedger);
        report.put("products", productList);
        report.put("customers", customerSummaryList);
        report.put("customerSummary", customerSummaryList);

        return report;
    }

    public List<SatisfactionReportDto> getSatisfactionDetails(String period) {
        Instant cutoff = getCutoffForPeriod(period);
        List<SatisfactionReportDto> result = new ArrayList<>();

        List<CustomerFeedback> feedbacks = feedbackRepository.findAllWithDetails();
        for (CustomerFeedback f : feedbacks) {
            if (cutoff != null && (f.getFeedbackDate() == null || !f.getFeedbackDate().isAfter(cutoff))) continue;
            SatisfactionReportDto dto = new SatisfactionReportDto();
            dto.setId(f.getId());
            if (f.getCustomer() != null) {
                dto.setCustomerId(f.getCustomer().getId());
                dto.setCustomerName(f.getCustomer().getName());
                dto.setPhoneNumber(f.getCustomer().getPhoneNumber());
                dto.setCity(f.getCustomer().getCity());
            }
            if (f.getOrder() != null) {
                dto.setOrderId(f.getOrder().getId());
                dto.setOrderNumber(f.getOrder().getOrderNumber());
            }
            dto.setRating(f.getRating());
            dto.setComment(f.getComment());
            dto.setFeedbackDate(f.getFeedbackDate());
            result.add(dto);
        }

        List<TimeFilterPreset> activePresets = presetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        java.util.Set<Long> activePresetIds = new java.util.HashSet<>();
        java.util.Set<String> activePresetNames = new java.util.HashSet<>();
        for (TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getName() != null) activePresetNames.add(p.getName().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        List<OrderFollowupCheck> checks = followupCheckRepository.findAllWithOrderCustomerAndPreset();
        for (OrderFollowupCheck c : checks) {
            boolean isPresetActive = (c.getPreset() != null && activePresetIds.contains(c.getPreset().getId()))
                    || (c.getMilestoneName() != null && activePresetNames.contains(c.getMilestoneName().trim().toLowerCase()));
            if (!isPresetActive) continue;
            if (c.getSatisfaction() == null || c.getSatisfaction().trim().isEmpty()) continue;
            Instant checkDate = c.getCheckedAt() != null ? c.getCheckedAt() :
                                (c.getUpdatedAt() != null ? c.getUpdatedAt() :
                                (c.getCompletedDate() != null ? c.getCompletedDate() :
                                (c.getOrder() != null ? c.getOrder().getOrderDate() : null)));
            if (cutoff != null && (checkDate == null || !checkDate.isAfter(cutoff))) continue;

            SatisfactionReportDto dto = new SatisfactionReportDto();
            dto.setId(c.getId());
            CustomerOrder ord = c.getOrder();
            Customer cust = ord != null ? ord.getCustomer() : null;
            if (cust != null) {
                dto.setCustomerId(cust.getId());
                dto.setCustomerName(cust.getName());
                dto.setPhoneNumber(cust.getPhoneNumber());
                dto.setCity(cust.getCity() != null ? cust.getCity() : "Erbil");
            }
            if (ord != null) {
                dto.setOrderId(ord.getId());
                dto.setOrderNumber(ord.getOrderNumber());
            }

            String s = c.getSatisfaction().toUpperCase();
            int rating = switch (s) {
                case "SATISFIED", "5" -> 5;
                case "4" -> 4;
                case "NEUTRAL", "3" -> 3;
                case "2" -> 2;
                case "UNSATISFIED", "1" -> 1;
                default -> 3;
            };
            dto.setRating(rating);
            dto.setSatisfaction(c.getSatisfaction());
            dto.setMilestoneName(c.getMilestoneName());
            String note = (c.getNotes() != null && !c.getNotes().trim().isEmpty()) ? c.getNotes() : ("Milestone: " + c.getMilestoneName());
            dto.setComment(note);
            dto.setFeedbackDate(checkDate != null ? checkDate : Instant.now());
            dto.setFollowupCheckedBy(c.getCheckedBy() != null ? c.getCheckedBy() : "Agent");
            result.add(dto);
        }

        result.sort((a, b) -> {
            if (a.getFeedbackDate() == null && b.getFeedbackDate() == null) return 0;
            if (a.getFeedbackDate() == null) return 1;
            if (b.getFeedbackDate() == null) return -1;
            return b.getFeedbackDate().compareTo(a.getFeedbackDate());
        });

        return result;
    }

    private Instant getCutoffForPeriod(String period) {
        return DateUtil.getFilterCutoff(period);
    }
}

