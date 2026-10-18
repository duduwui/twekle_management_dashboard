package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.SatisfactionReportDto;
import com.twekl.dashboard.model.administration.Customer;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;
import com.twekl.dashboard.model.administration.OrderFollowupCheck;
import com.twekl.dashboard.model.administration.TimeFilterPreset;
import com.twekl.dashboard.repository.administration.CustomerFeedbackRepository;
import com.twekl.dashboard.repository.administration.CustomerOrderRepository;
import com.twekl.dashboard.repository.administration.CustomerRepository;
import com.twekl.dashboard.repository.administration.OrderFollowupCheckRepository;
import com.twekl.dashboard.repository.administration.TimeFilterPresetRepository;
import com.twekl.dashboard.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics and satisfaction reporting.
 * Consolidated single service file with final autowired dependencies.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final TimeFilterPresetRepository presetRepository;

    @Autowired
    public ReportService(final CustomerRepository customerRepository,
                         final CustomerOrderRepository orderRepository,
                         final CustomerFeedbackRepository feedbackRepository,
                         final OrderFollowupCheckRepository followupCheckRepository,
                         final TimeFilterPresetRepository presetRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.presetRepository = presetRepository;
    }

    public Map<String, Object> generateReport(String period) {
        return generateReport(period, null, null);
    }

    @Cacheable(value = "reportData", key = "(#period != null ? #period : 'ALL') + '_' + (#dateFrom != null ? #dateFrom : '') + '_' + (#dateTo != null ? #dateTo : '')", sync = true)
    public Map<String, Object> generateReport(String period, String dateFrom, String dateTo) {
        Instant startDate = null;
        Instant endDate = null;
        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            try {
                String dStr = dateFrom.trim().substring(0, 10);
                LocalDate ld = LocalDate.parse(dStr);
                startDate = ld.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {}
        }
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            try {
                String dStr = dateTo.trim().substring(0, 10);
                LocalDate ld = LocalDate.parse(dStr);
                endDate = ld.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {}
        }

        Instant cutoff = startDate == null ? getCutoffForPeriod(period) : null;
        boolean hasDateRange = (startDate != null);
        Instant rangeEnd = endDate != null ? endDate : Instant.now();

        // 1. High-level Summary Metrics using fast SQL aggregations
        long newCustomers = hasDateRange
                ? customerRepository.countByCreatedAtBetween(startDate, rangeEnd)
                : (cutoff == null ? customerRepository.count() : customerRepository.countByCreatedAtAfter(cutoff));

        long totalOrdersInPeriod = hasDateRange
                ? orderRepository.countByOrderDateBetween(startDate, rangeEnd)
                : (cutoff == null ? orderRepository.count() : orderRepository.countByOrderDateAfter(cutoff));

        double revenueInPeriod = hasDateRange
                ? orderRepository.sumRevenueByDateRange(startDate, rangeEnd)
                : (cutoff == null ? orderRepository.sumTotalRevenue() : orderRepository.sumRevenueByPeriod(cutoff));

        List<Customer> allCustomers = hasDateRange
                ? customerRepository.findByLastOrderDateBetween(startDate, rangeEnd, org.springframework.data.domain.PageRequest.of(0, 200, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastOrderDate")))
                : (cutoff == null
                    ? customerRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 200, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))).getContent()
                    : customerRepository.findByLastOrderDateAfter(cutoff, org.springframework.data.domain.PageRequest.of(0, 200, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastOrderDate"))));

        List<CustomerOrder> filteredOrders = hasDateRange
                ? orderRepository.findRecentOrdersWithCustomerByDateRange(startDate, rangeEnd, org.springframework.data.domain.PageRequest.of(0, 300))
                : orderRepository.findRecentOrdersWithCustomer(cutoff, org.springframework.data.domain.PageRequest.of(0, 300));

        List<Long> orderIds = filteredOrders.stream().map(CustomerOrder::getId).filter(Objects::nonNull).toList();
        List<OrderFollowupCheck> orderChecks = orderIds.isEmpty() ? Collections.emptyList() : followupCheckRepository.findByOrderIds(orderIds);
        Map<Long, List<OrderFollowupCheck>> checksByOrderId = orderChecks.stream()
                .filter(c -> c.getOrder() != null && c.getOrder().getId() != null)
                .collect(Collectors.groupingBy(c -> c.getOrder().getId()));

        List<CustomerFeedback> allFeedbacks = feedbackRepository.findRecentFeedbacks(cutoff != null ? cutoff : startDate, org.springframework.data.domain.PageRequest.of(0, 200));
        double avgRating = feedbackRepository.getAverageRating(cutoff != null ? cutoff : startDate);

        // 2. Milestones Matrix using fast, 100% accurate SQL aggregations (covering index)
        List<Object[]> rawMilestoneStats = hasDateRange
                ? followupCheckRepository.getMilestoneStatsByDateRange(startDate, rangeEnd)
                : (cutoff == null ? followupCheckRepository.getMilestoneStatsAll() : followupCheckRepository.getMilestoneStatsByPeriod(cutoff));

        Map<String, long[]> dbMilestoneCounts = new LinkedHashMap<>();
        for (Object[] row : rawMilestoneStats) {
            String name = (String) row[0];
            if (name != null) {
                long tot = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                long comp = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                long pend = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                dbMilestoneCounts.put(name.trim().toLowerCase(), new long[]{tot, comp, pend});
            }
        }

        List<TimeFilterPreset> activePresets = presetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        activePresets.sort(Comparator.comparingInt(TimeFilterPreset::getEquivalentDurationMinutes));

        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getName() != null) activePresetNames.add(p.getName().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        List<Map<String, Object>> milestoneList = new ArrayList<>();
        Set<String> matchedPresets = new HashSet<>();

        for (TimeFilterPreset p : activePresets) {
            String key = p.getName() != null ? p.getName().trim().toLowerCase() : "";
            long[] counts = dbMilestoneCounts.getOrDefault(key, new long[]{0L, 0L, 0L});
            matchedPresets.add(key);

            long tot = counts[0];
            long comp = counts[1];
            long pend = counts[2];
            double pct = tot > 0 ? Math.round(((double) comp / tot * 1000.0)) / 10.0 : 0.0;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("presetName", p.getName());
            m.put("total", tot);
            m.put("completed", comp);
            m.put("pending", pend);
            m.put("completionRate", pct);
            milestoneList.add(m);
        }

        for (Map.Entry<String, long[]> entry : dbMilestoneCounts.entrySet()) {
            if (!matchedPresets.contains(entry.getKey())) {
                long tot = entry.getValue()[0];
                long comp = entry.getValue()[1];
                long pend = entry.getValue()[2];
                double pct = tot > 0 ? Math.round(((double) comp / tot * 1000.0)) / 10.0 : 0.0;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("presetName", entry.getKey());
                m.put("total", tot);
                m.put("completed", comp);
                m.put("pending", pend);
                m.put("completionRate", pct);
                milestoneList.add(m);
            }
        }

        // Summary follow-up metrics across entire database
        long completedChecks = hasDateRange
                ? followupCheckRepository.countCompletedByDateRange(startDate, rangeEnd)
                : (cutoff == null ? followupCheckRepository.countByCompletedTrue() : followupCheckRepository.countCompletedByPeriod(cutoff));

        long totalChecks = hasDateRange
                ? followupCheckRepository.countTotalByDateRange(startDate, rangeEnd)
                : (cutoff == null ? followupCheckRepository.count() : followupCheckRepository.countTotalByPeriod(cutoff));

        double completionRate = totalChecks > 0 ? Math.round(((double) completedChecks / totalChecks * 1000.0)) / 10.0 : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("newCustomers", newCustomers);
        summary.put("totalOrders", totalOrdersInPeriod);
        summary.put("revenue", Math.round(revenueInPeriod * 100.0) / 100.0);
        summary.put("completedFollowups", completedChecks);
        summary.put("completionRate", completionRate);
        summary.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        summary.put("totalFeedbacks", allFeedbacks.size());

        // 3. Customer Satisfaction & Notes Ledger
        long satCount = hasDateRange
                ? followupCheckRepository.countSatisfiedByDateRange(startDate, rangeEnd)
                : (cutoff == null ? followupCheckRepository.countSatisfied() : followupCheckRepository.countSatisfiedByPeriod(cutoff));

        long neuCount = hasDateRange
                ? followupCheckRepository.countNeutralByDateRange(startDate, rangeEnd)
                : (cutoff == null ? followupCheckRepository.countNeutral() : followupCheckRepository.countNeutralByPeriod(cutoff));

        long unsatCount = hasDateRange
                ? followupCheckRepository.countUnsatisfiedByDateRange(startDate, rangeEnd)
                : (cutoff == null ? followupCheckRepository.countUnsatisfied() : followupCheckRepository.countUnsatisfiedByPeriod(cutoff));

        long totalRated = satCount + neuCount + unsatCount;
        double positiveRate = totalRated > 0 ? Math.round(((double) satCount / totalRated * 1000.0)) / 10.0 : 100.0;

        // Pending follow-up checks for the Pending Contacts table
        List<OrderFollowupCheck> pendingChecks = hasDateRange
                ? followupCheckRepository.findPendingChecksByDateRange(startDate, rangeEnd, org.springframework.data.domain.PageRequest.of(0, 200))
                : followupCheckRepository.findPendingChecks(cutoff, org.springframework.data.domain.PageRequest.of(0, 200));

        List<Map<String, Object>> pendingList = new ArrayList<>();
        for (OrderFollowupCheck c : pendingChecks) {
            CustomerOrder ord = c.getOrder();
            Customer cust = ord != null ? ord.getCustomer() : null;
            Map<String, Object> pItem = new LinkedHashMap<>();
            pItem.put("id", c.getId());
            pItem.put("orderId", ord != null ? ord.getId() : null);
            pItem.put("orderNumber", ord != null ? ord.getOrderNumber() : "-");
            pItem.put("customerName", cust != null ? cust.getName() : "Unknown");
            pItem.put("customerPhone", cust != null ? cust.getPhoneNumber() : "-");
            pItem.put("presetName", c.getMilestoneName() != null ? c.getMilestoneName() : "Follow-up");
            pItem.put("status", c.getStatus() != null ? c.getStatus() : "PENDING");
            pendingList.add(pItem);
        }

        List<OrderFollowupCheck> checksWithSat = followupCheckRepository.findChecksWithSatisfaction(org.springframework.data.domain.PageRequest.of(0, 100));
        List<Map<String, Object>> notesLedger = new ArrayList<>();
        for (OrderFollowupCheck c : checksWithSat) {
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

        Map<String, Object> satisfactionMap = new LinkedHashMap<>();
        satisfactionMap.put("recentNotes", notesLedger);
        satisfactionMap.put("totalRated", totalRated);
        satisfactionMap.put("satisfiedCount", satCount);
        satisfactionMap.put("neutralCount", neuCount);
        satisfactionMap.put("unsatisfiedCount", unsatCount);
        satisfactionMap.put("positiveRate", positiveRate);

        double negativeRate = totalRated > 0 ? Math.round(((double) unsatCount / totalRated * 1000.0)) / 10.0 : 0.0;
        double netSentimentScore = Math.round((positiveRate - negativeRate) * 10.0) / 10.0;

        summary.put("satisfactionRate", positiveRate);
        summary.put("negativeRate", negativeRate);
        summary.put("netSentimentScore", netSentimentScore);
        summary.put("satisfiedCount", satCount);
        summary.put("neutralCount", neuCount);
        summary.put("unsatisfiedCount", unsatCount);
        summary.put("totalRated", totalRated);

        // ---------------------------------------------------------------------
        // 4. FINANCIAL INTELLIGENCE & REVENUE METRICS
        // ---------------------------------------------------------------------
        List<Object[]> satFinStats = followupCheckRepository.getSatisfactionFinancialStatsByDateRange(startDate, rangeEnd);
        double satisfiedRevenue = 0.0;
        double neutralRevenue = 0.0;
        double unsatisfiedRevenue = 0.0;
        long satFinCount = 0;
        long neuFinCount = 0;
        long unsatFinCount = 0;

        for (Object[] row : satFinStats) {
            String s = row[0] != null ? row[0].toString().trim().toUpperCase() : "";
            long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            double rev = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            if ("SATISFIED".equals(s) || "4".equals(s) || "5".equals(s)) {
                satisfiedRevenue += rev;
                satFinCount += cnt;
            } else if ("NEUTRAL".equals(s) || "3".equals(s)) {
                neutralRevenue += rev;
                neuFinCount += cnt;
            } else if ("UNSATISFIED".equals(s) || "1".equals(s) || "2".equals(s)) {
                unsatisfiedRevenue += rev;
                unsatFinCount += cnt;
            }
        }

        List<Object[]> repeatStats = orderRepository.getRepeatOrderStatsByDateRange(startDate, rangeEnd);
        long repeatOrders = 0L;
        double repeatRevenue = 0.0;
        if (!repeatStats.isEmpty() && repeatStats.get(0) != null) {
            Object[] rRow = repeatStats.get(0);
            repeatOrders = rRow[0] != null ? ((Number) rRow[0]).longValue() : 0L;
            repeatRevenue = rRow[1] != null ? ((Number) rRow[1]).doubleValue() : 0.0;
        }

        double avgOrderValue = totalOrdersInPeriod > 0 ? Math.round((revenueInPeriod / totalOrdersInPeriod) * 100.0) / 100.0 : 0.0;
        double arpu = newCustomers > 0 ? Math.round((revenueInPeriod / newCustomers) * 100.0) / 100.0 : 0.0;
        double repeatRevenueShare = revenueInPeriod > 0 ? Math.round((repeatRevenue / revenueInPeriod * 1000.0)) / 10.0 : 0.0;

        // Customer Tiers (VIP, Gold, Silver, Bronze)
        List<Object[]> tierStats = customerRepository.getCustomerTierStats();
        List<Map<String, Object>> tierList = new ArrayList<>();
        if (!tierStats.isEmpty() && tierStats.get(0) != null) {
            Object[] tRow = tierStats.get(0);
            long vipCnt = tRow[0] != null ? ((Number) tRow[0]).longValue() : 0L;
            double vipRev = tRow[1] != null ? ((Number) tRow[1]).doubleValue() : 0.0;
            long goldCnt = tRow[2] != null ? ((Number) tRow[2]).longValue() : 0L;
            double goldRev = tRow[3] != null ? ((Number) tRow[3]).doubleValue() : 0.0;
            long silvCnt = tRow[4] != null ? ((Number) tRow[4]).longValue() : 0L;
            double silvRev = tRow[5] != null ? ((Number) tRow[5]).doubleValue() : 0.0;
            long bronzCnt = tRow[6] != null ? ((Number) tRow[6]).longValue() : 0L;
            double bronzRev = tRow[7] != null ? ((Number) tRow[7]).doubleValue() : 0.0;

            double totTierRev = vipRev + goldRev + silvRev + bronzRev;
            if (totTierRev == 0.0) totTierRev = 1.0;

            Map<String, Object> vipMap = new LinkedHashMap<>();
            vipMap.put("tierName", "VIP Platinum (> $5,000)");
            vipMap.put("customerCount", vipCnt);
            vipMap.put("totalRevenue", Math.round(vipRev * 100.0) / 100.0);
            vipMap.put("revenueShare", Math.round((vipRev / totTierRev * 1000.0)) / 10.0);
            vipMap.put("avgSpend", vipCnt > 0 ? Math.round((vipRev / vipCnt) * 100.0) / 100.0 : 0.0);
            vipMap.put("status", "High Priority");
            tierList.add(vipMap);

            Map<String, Object> goldMap = new LinkedHashMap<>();
            goldMap.put("tierName", "Gold Tier ($1,000 - $5,000)");
            goldMap.put("customerCount", goldCnt);
            goldMap.put("totalRevenue", Math.round(goldRev * 100.0) / 100.0);
            goldMap.put("revenueShare", Math.round((goldRev / totTierRev * 1000.0)) / 10.0);
            goldMap.put("avgSpend", goldCnt > 0 ? Math.round((goldRev / goldCnt) * 100.0) / 100.0 : 0.0);
            goldMap.put("status", "Core Business");
            tierList.add(goldMap);

            Map<String, Object> silvMap = new LinkedHashMap<>();
            silvMap.put("tierName", "Silver Tier ($250 - $1,000)");
            silvMap.put("customerCount", silvCnt);
            silvMap.put("totalRevenue", Math.round(silvRev * 100.0) / 100.0);
            silvMap.put("revenueShare", Math.round((silvRev / totTierRev * 1000.0)) / 10.0);
            silvMap.put("avgSpend", silvCnt > 0 ? Math.round((silvRev / silvCnt) * 100.0) / 100.0 : 0.0);
            silvMap.put("status", "Growth Opportunity");
            tierList.add(silvMap);

            Map<String, Object> bronzMap = new LinkedHashMap<>();
            bronzMap.put("tierName", "Bronze Tier (< $250)");
            bronzMap.put("customerCount", bronzCnt);
            bronzMap.put("totalRevenue", Math.round(bronzRev * 100.0) / 100.0);
            bronzMap.put("revenueShare", Math.round((bronzRev / totTierRev * 1000.0)) / 10.0);
            bronzMap.put("avgSpend", bronzCnt > 0 ? Math.round((bronzRev / bronzCnt) * 100.0) / 100.0 : 0.0);
            bronzMap.put("status", "New / Entry Level");
            tierList.add(bronzMap);
        }

        // Financial Impact of Customer Feedback
        List<Map<String, Object>> feedbackImpactList = new ArrayList<>();
        double totalFeedbackRev = satisfiedRevenue + neutralRevenue + unsatisfiedRevenue;
        if (totalFeedbackRev == 0.0) totalFeedbackRev = 1.0;

        Map<String, Object> satImpact = new LinkedHashMap<>();
        satImpact.put("feedbackType", "Satisfied Customers");
        satImpact.put("checkCount", satFinCount);
        satImpact.put("totalRevenue", Math.round(satisfiedRevenue * 100.0) / 100.0);
        satImpact.put("revenueShare", Math.round((satisfiedRevenue / totalFeedbackRev * 1000.0)) / 10.0);
        satImpact.put("avgOrderValue", satFinCount > 0 ? Math.round((satisfiedRevenue / satFinCount) * 100.0) / 100.0 : 0.0);
        satImpact.put("impactStatus", "Retained & Active");
        feedbackImpactList.add(satImpact);

        Map<String, Object> neuImpact = new LinkedHashMap<>();
        neuImpact.put("feedbackType", "Neutral Customers");
        neuImpact.put("checkCount", neuFinCount);
        neuImpact.put("totalRevenue", Math.round(neutralRevenue * 100.0) / 100.0);
        neuImpact.put("revenueShare", Math.round((neutralRevenue / totalFeedbackRev * 1000.0)) / 10.0);
        neuImpact.put("avgOrderValue", neuFinCount > 0 ? Math.round((neutralRevenue / neuFinCount) * 100.0) / 100.0 : 0.0);
        neuImpact.put("impactStatus", "Upsell Potential");
        feedbackImpactList.add(neuImpact);

        Map<String, Object> unsatImpact = new LinkedHashMap<>();
        unsatImpact.put("feedbackType", "Unsatisfied / Complaints");
        unsatImpact.put("checkCount", unsatFinCount);
        unsatImpact.put("totalRevenue", Math.round(unsatisfiedRevenue * 100.0) / 100.0);
        unsatImpact.put("revenueShare", Math.round((unsatisfiedRevenue / totalFeedbackRev * 1000.0)) / 10.0);
        unsatImpact.put("avgOrderValue", unsatFinCount > 0 ? Math.round((unsatisfiedRevenue / unsatFinCount) * 100.0) / 100.0 : 0.0);
        unsatImpact.put("impactStatus", "Revenue at Churn Risk");
        feedbackImpactList.add(unsatImpact);

        Map<String, Object> financial = new LinkedHashMap<>();
        financial.put("totalRevenue", Math.round(revenueInPeriod * 100.0) / 100.0);
        financial.put("totalOrders", totalOrdersInPeriod);
        financial.put("avgOrderValue", avgOrderValue);
        financial.put("arpu", arpu);
        financial.put("repeatRevenue", Math.round(repeatRevenue * 100.0) / 100.0);
        financial.put("repeatOrders", repeatOrders);
        financial.put("repeatRevenueShare", repeatRevenueShare);
        financial.put("revenueAtRisk", Math.round(unsatisfiedRevenue * 100.0) / 100.0);
        financial.put("satisfiedRevenue", Math.round(satisfiedRevenue * 100.0) / 100.0);
        financial.put("neutralRevenue", Math.round(neutralRevenue * 100.0) / 100.0);
        financial.put("tiers", tierList);
        financial.put("feedbackImpact", feedbackImpactList);

        summary.put("avgOrderValue", avgOrderValue);
        summary.put("arpu", arpu);
        summary.put("repeatRevenue", Math.round(repeatRevenue * 100.0) / 100.0);
        summary.put("repeatRevenueShare", repeatRevenueShare);
        summary.put("revenueAtRisk", Math.round(unsatisfiedRevenue * 100.0) / 100.0);

        // ---------------------------------------------------------------------
        // 5. ACCURATE PRODUCT DISTRIBUTION & REVENUE
        // ---------------------------------------------------------------------
        List<Object[]> rawProdFinancials = orderRepository.getProductFinancialStatsByDateRange(startDate, rangeEnd);
        List<Object[]> rawProdReviews = followupCheckRepository.getProductSatisfactionCountsByDateRange(startDate, rangeEnd);

        Map<String, long[]> prodReviewMap = new HashMap<>();
        for (Object[] row : rawProdReviews) {
            String pName = row[0] != null ? row[0].toString().trim() : "";
            if (!pName.isEmpty()) {
                long sCnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                long nCnt = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                long uCnt = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                long totChk = row[4] != null ? ((Number) row[4]).longValue() : 0L;
                prodReviewMap.put(pName, new long[]{sCnt, nCnt, uCnt, totChk});
            }
        }

        List<Map<String, Object>> productList = new ArrayList<>();
        for (Object[] row : rawProdFinancials) {
            String pName = row[0] != null ? row[0].toString().trim() : "Standard Item";
            long ordCnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            double totRev = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double avgTicket = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;

            long[] rStats = prodReviewMap.getOrDefault(pName, new long[]{0L, 0L, 0L, 0L});
            long sCnt = rStats[0];
            long nCnt = rStats[1];
            long uCnt = rStats[2];
            long totRevCnt = sCnt + nCnt + uCnt;
            double defectRate = totRevCnt > 0 ? Math.round(((double) uCnt / totRevCnt * 1000.0)) / 10.0 : 0.0;
            double revShare = revenueInPeriod > 0 ? Math.round((totRev / revenueInPeriod * 1000.0)) / 10.0 : 0.0;

            Map<String, Object> pItem = new LinkedHashMap<>();
            pItem.put("productName", pName);
            pItem.put("orderCount", ordCnt);
            pItem.put("totalRevenue", Math.round(totRev * 100.0) / 100.0);
            pItem.put("avgTicket", Math.round(avgTicket * 100.0) / 100.0);
            pItem.put("revenueShare", revShare);
            pItem.put("satisfiedCount", sCnt);
            pItem.put("neutralCount", nCnt);
            pItem.put("unsatisfiedCount", uCnt);
            pItem.put("notesCount", totRevCnt);
            pItem.put("defectRate", defectRate);
            productList.add(pItem);
        }

        // 6. Customer Directory Summary
        Map<Long, List<CustomerOrder>> ordersByCustomerId = new HashMap<>();
        for (CustomerOrder o : filteredOrders) {
            if (o.getCustomer() != null && o.getCustomer().getId() != null) {
                ordersByCustomerId.computeIfAbsent(o.getCustomer().getId(), k -> new ArrayList<>()).add(o);
            }
        }

        Map<Long, Map<String, Object>> customerSummaryMap = new LinkedHashMap<>();
        for (Customer cust : allCustomers) {
            List<CustomerOrder> custOrders = ordersByCustomerId.getOrDefault(cust.getId(), Collections.emptyList());

            if (cutoff != null && custOrders.isEmpty()) continue;

            Map<String, Object> cData = new LinkedHashMap<>();
            cData.put("id", cust.getId());
            cData.put("name", cust.getName());
            cData.put("phoneNumber", cust.getPhoneNumber());
            cData.put("city", cust.getCity() != null ? cust.getCity() : "Erbil");
            cData.put("totalOrders", custOrders.size());
            double totalSpent = custOrders.stream().mapToDouble(CustomerOrder::getTotalAmount).sum();
            cData.put("totalSpent", Math.round(totalSpent * 100.0) / 100.0);

            // Tier assignment
            String tier = "Bronze";
            if (totalSpent >= 5000) tier = "VIP Platinum";
            else if (totalSpent >= 1000) tier = "Gold";
            else if (totalSpent >= 250) tier = "Silver";
            cData.put("tier", tier);

            int pendingFollowups = 0;
            for (CustomerOrder o : custOrders) {
                List<OrderFollowupCheck> oChecks = checksByOrderId.getOrDefault(o.getId(), Collections.emptyList());
                for (OrderFollowupCheck chk : oChecks) {
                    boolean isPresetActive = (chk.getPreset() != null && activePresetIds.contains(chk.getPreset().getId()))
                            || (chk.getMilestoneName() != null && activePresetNames.contains(chk.getMilestoneName().trim().toLowerCase()));
                    if (isPresetActive && !chk.isCompleted()) {
                        pendingFollowups++;
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
        report.put("financial", financial);
        report.put("milestones", milestoneList);
        report.put("satisfaction", satisfactionMap);
        report.put("satisfactionNotes", notesLedger);
        report.put("products", productList);
        report.put("customers", customerSummaryList);
        report.put("customerSummary", customerSummaryList);
        report.put("pendingContacts", pendingList);

        return report;
    }

    @Cacheable(value = "satisfactionDetails", key = "#period != null ? #period : 'ALL'", sync = true)
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
        Set<Long> activePresetIds = new HashSet<>();
        Set<String> activePresetNames = new HashSet<>();
        for (TimeFilterPreset p : activePresets) {
            if (p.getId() != null) activePresetIds.add(p.getId());
            if (p.getLabelEn() != null) activePresetNames.add(p.getLabelEn().trim().toLowerCase());
            if (p.getName() != null) activePresetNames.add(p.getName().trim().toLowerCase());
            if (p.getPresetKey() != null) activePresetNames.add(p.getPresetKey().trim().toLowerCase());
        }

        List<OrderFollowupCheck> checks = followupCheckRepository.findChecksWithSatisfaction(org.springframework.data.domain.PageRequest.of(0, 300));
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
