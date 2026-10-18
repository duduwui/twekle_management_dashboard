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

    private static class CachedLoyaltyTiers {
        final Map<String, Object> loyaltyTiers;
        final long timestamp;
        CachedLoyaltyTiers(Map<String, Object> loyaltyTiers, long timestamp) {
            this.loyaltyTiers = loyaltyTiers;
            this.timestamp = timestamp;
        }
    }
    private volatile CachedLoyaltyTiers cachedLoyaltyTiers = null;

    private static class CachedProductReviews {
        final Map<String, long[]> reviewMap;
        final long timestamp;
        CachedProductReviews(Map<String, long[]> reviewMap, long timestamp) {
            this.reviewMap = reviewMap;
            this.timestamp = timestamp;
        }
    }
    private final java.util.concurrent.ConcurrentMap<String, CachedProductReviews> productReviewsCache = new java.util.concurrent.ConcurrentHashMap<>();

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

        List<Customer> allCustomers = hasDateRange
                ? customerRepository.findByLastOrderDateBetween(startDate, rangeEnd, org.springframework.data.domain.PageRequest.of(0, 500, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastOrderDate")))
                : (cutoff == null
                    ? customerRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 500, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))).getContent()
                    : customerRepository.findByLastOrderDateAfter(cutoff, org.springframework.data.domain.PageRequest.of(0, 500, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastOrderDate"))));

        List<Long> customerIds = allCustomers.stream().map(Customer::getId).filter(Objects::nonNull).toList();
        List<CustomerOrder> filteredOrders = customerIds.isEmpty() ? Collections.emptyList() : orderRepository.findByCustomerIds(customerIds);

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
        long totalChecks = 0L;
        long completedChecks = 0L;
        for (Object[] row : rawMilestoneStats) {
            String name = (String) row[0];
            if (name != null) {
                long tot = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                long comp = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                long pend = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                dbMilestoneCounts.put(name.trim().toLowerCase(), new long[]{tot, comp, pend});
                totalChecks += tot;
                completedChecks += comp;
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
        double completionRate = totalChecks > 0 ? Math.round(((double) completedChecks / totalChecks * 1000.0)) / 10.0 : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("newCustomers", newCustomers);
        summary.put("totalOrders", totalOrdersInPeriod);
        summary.put("completedFollowups", completedChecks);
        summary.put("completionRate", completionRate);
        summary.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        summary.put("totalFeedbacks", allFeedbacks.size());

        // 3. Customer Satisfaction & Notes Ledger (Single Combined Query)
        long satCount = 0L;
        long neuCount = 0L;
        long unsatCount = 0L;
        List<Object[]> satCountsList = followupCheckRepository.getSatisfactionCountsCombined(hasDateRange ? startDate : cutoff, hasDateRange ? rangeEnd : null);
        if (satCountsList != null && !satCountsList.isEmpty() && satCountsList.get(0) != null) {
            Object[] scRow = satCountsList.get(0);
            satCount = scRow[0] != null ? ((Number) scRow[0]).longValue() : 0L;
            neuCount = scRow[1] != null ? ((Number) scRow[1]).longValue() : 0L;
            unsatCount = scRow[2] != null ? ((Number) scRow[2]).longValue() : 0L;
        }

        long totalRated = satCount + neuCount + unsatCount;
        double positiveRate = totalRated > 0 ? Math.round(((double) satCount / totalRated * 1000.0)) / 10.0 : 100.0;

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
        // 4. CUSTOMER RETENTION & ORDER LOYALTY TIERS (Frequency & Repetition)
        // ---------------------------------------------------------------------
        long nowMs = System.currentTimeMillis();
        Map<String, Object> loyaltyTiers;
        CachedLoyaltyTiers clt = cachedLoyaltyTiers;
        if (clt != null && (nowMs - clt.timestamp) < 60_000L) {
            loyaltyTiers = clt.loyaltyTiers;
        } else {
            List<Object[]> tierStats = customerRepository.getCustomerTierStats();
            List<Map<String, Object>> loyaltyTierList = new ArrayList<>();
            long repeatCustomerCount = 0L;
            long oneTimeCustomerCount = 0L;
            long totalCatalogCustomers = 0L;

            if (!tierStats.isEmpty() && tierStats.get(0) != null) {
                Object[] tRow = tierStats.get(0);
                long vipCnt = tRow[0] != null ? ((Number) tRow[0]).longValue() : 0L;
                long vipOrders = tRow[1] != null ? ((Number) tRow[1]).longValue() : 0L;
                long freqCnt = tRow[2] != null ? ((Number) tRow[2]).longValue() : 0L;
                long freqOrders = tRow[3] != null ? ((Number) tRow[3]).longValue() : 0L;
                long retCnt = tRow[4] != null ? ((Number) tRow[4]).longValue() : 0L;
                long retOrders = tRow[5] != null ? ((Number) tRow[5]).longValue() : 0L;
                long oneCnt = tRow[6] != null ? ((Number) tRow[6]).longValue() : 0L;
                long oneOrders = tRow[7] != null ? ((Number) tRow[7]).longValue() : 0L;

                repeatCustomerCount = vipCnt + freqCnt + retCnt;
                oneTimeCustomerCount = oneCnt;
                totalCatalogCustomers = repeatCustomerCount + oneTimeCustomerCount;
                if (totalCatalogCustomers == 0L) totalCatalogCustomers = 1L;

                Map<String, Object> vipMap = new LinkedHashMap<>();
                vipMap.put("tierName", "VIP Champions (5+ Orders)");
                vipMap.put("tierKey", "VIP_CHAMPION");
                vipMap.put("customerCount", vipCnt);
                vipMap.put("totalOrders", vipOrders);
                vipMap.put("customerShare", Math.round(((double) vipCnt / totalCatalogCustomers * 1000.0)) / 10.0);
                vipMap.put("avgOrdersPerCustomer", vipCnt > 0 ? Math.round(((double) vipOrders / vipCnt * 10.0)) / 10.0 : 0.0);
                vipMap.put("retentionStatus", "Highly Loyal Advocates");
                vipMap.put("badgeColor", "#6B21A8");
                vipMap.put("badgeBg", "#F3E8FF");
                loyaltyTierList.add(vipMap);

                Map<String, Object> freqMap = new LinkedHashMap<>();
                freqMap.put("tierName", "Frequent Buyers (3-4 Orders)");
                freqMap.put("tierKey", "FREQUENT_BUYER");
                freqMap.put("customerCount", freqCnt);
                freqMap.put("totalOrders", freqOrders);
                freqMap.put("customerShare", Math.round(((double) freqCnt / totalCatalogCustomers * 1000.0)) / 10.0);
                freqMap.put("avgOrdersPerCustomer", freqCnt > 0 ? Math.round(((double) freqOrders / freqCnt * 10.0)) / 10.0 : 0.0);
                freqMap.put("retentionStatus", "Consistent Repeat Buyers");
                freqMap.put("badgeColor", "#15803D");
                freqMap.put("badgeBg", "#DCFCE7");
                loyaltyTierList.add(freqMap);

                Map<String, Object> retMap = new LinkedHashMap<>();
                retMap.put("tierName", "Returning Buyers (2 Orders)");
                retMap.put("tierKey", "RETURNING_BUYER");
                retMap.put("customerCount", retCnt);
                retMap.put("totalOrders", retOrders);
                retMap.put("customerShare", Math.round(((double) retCnt / totalCatalogCustomers * 1000.0)) / 10.0);
                retMap.put("avgOrdersPerCustomer", retCnt > 0 ? Math.round(((double) retOrders / retCnt * 10.0)) / 10.0 : 0.0);
                retMap.put("retentionStatus", "Second-Time Converted");
                retMap.put("badgeColor", "#0369A1");
                retMap.put("badgeBg", "#E0F2FE");
                loyaltyTierList.add(retMap);

                Map<String, Object> oneMap = new LinkedHashMap<>();
                oneMap.put("tierName", "One-Time Buyers (1 Order)");
                oneMap.put("tierKey", "ONE_TIME_BUYER");
                oneMap.put("customerCount", oneCnt);
                oneMap.put("totalOrders", oneOrders);
                oneMap.put("customerShare", Math.round(((double) oneCnt / totalCatalogCustomers * 1000.0)) / 10.0);
                oneMap.put("avgOrdersPerCustomer", oneCnt > 0 ? Math.round(((double) oneOrders / oneCnt * 10.0)) / 10.0 : 0.0);
                oneMap.put("retentionStatus", "At-Risk (Single Purchase)");
                oneMap.put("badgeColor", "#B45309");
                oneMap.put("badgeBg", "#FEF3C7");
                loyaltyTierList.add(oneMap);
            }

            double repeatCustomerRate = totalCatalogCustomers > 0
                    ? Math.round(((double) repeatCustomerCount / totalCatalogCustomers * 1000.0)) / 10.0 : 0.0;
            double oneTimeRate = totalCatalogCustomers > 0
                    ? Math.round(((double) oneTimeCustomerCount / totalCatalogCustomers * 1000.0)) / 10.0 : 0.0;

            loyaltyTiers = new LinkedHashMap<>();
            loyaltyTiers.put("tiers", loyaltyTierList);
            loyaltyTiers.put("repeatCustomerCount", repeatCustomerCount);
            loyaltyTiers.put("repeatCustomerRate", repeatCustomerRate);
            loyaltyTiers.put("oneTimeCustomerCount", oneTimeCustomerCount);
            loyaltyTiers.put("oneTimeRate", oneTimeRate);
            loyaltyTiers.put("totalCustomers", totalCatalogCustomers);
            cachedLoyaltyTiers = new CachedLoyaltyTiers(loyaltyTiers, nowMs);
        }

        summary.put("repeatCustomerCount", loyaltyTiers.get("repeatCustomerCount"));
        summary.put("repeatCustomerRate", loyaltyTiers.get("repeatCustomerRate"));
        summary.put("oneTimeCustomerCount", loyaltyTiers.get("oneTimeCustomerCount"));
        summary.put("oneTimeRate", loyaltyTiers.get("oneTimeRate"));

        // ---------------------------------------------------------------------
        // 5. ACCURATE PRODUCT DISTRIBUTION & QUALITY HEALTH
        // ---------------------------------------------------------------------
        List<Object[]> rawProdFinancials = orderRepository.getProductFinancialStatsByDateRange(startDate, rangeEnd);
        String pCacheKey = (startDate != null ? startDate.toString() : "ALL") + "_" + (rangeEnd != null ? rangeEnd.toString() : "NOW");
        CachedProductReviews cpr = productReviewsCache.get(pCacheKey);
        Map<String, long[]> prodReviewMap;
        if (cpr != null && (nowMs - cpr.timestamp) < 60_000L) {
            prodReviewMap = cpr.reviewMap;
        } else {
            List<Object[]> rawProdReviews = followupCheckRepository.getProductSatisfactionCountsByDateRange(startDate, rangeEnd);
            prodReviewMap = new HashMap<>();
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
            productReviewsCache.put(pCacheKey, new CachedProductReviews(prodReviewMap, nowMs));
        }

        List<Map<String, Object>> productList = new ArrayList<>();
        for (Object[] row : rawProdFinancials) {
            String pName = row[0] != null ? row[0].toString().trim() : "Standard Item";
            long ordCnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;

            long[] rStats = prodReviewMap.getOrDefault(pName, new long[]{0L, 0L, 0L, 0L});
            long sCnt = rStats[0];
            long nCnt = rStats[1];
            long uCnt = rStats[2];
            long totRevCnt = sCnt + nCnt + uCnt;
            double defectRate = totRevCnt > 0 ? Math.round(((double) uCnt / totRevCnt * 1000.0)) / 10.0 : 0.0;

            Map<String, Object> pItem = new LinkedHashMap<>();
            pItem.put("productName", pName);
            pItem.put("orderCount", ordCnt);
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
            cData.put("phoneNumber", cust.getPhoneNumber() != null ? cust.getPhoneNumber() : "-");
            cData.put("city", cust.getCity() != null ? cust.getCity() : "Erbil");
            cData.put("email", cust.getEmail() != null ? cust.getEmail() : "-");

            int totalOrders = cust.getTotalOrders() > 0
                    ? cust.getTotalOrders()
                    : custOrders.size();
            cData.put("totalOrders", totalOrders);
            cData.put("ordersInPeriod", custOrders.size());

            double totalSpent = cust.getTotalSpent() > 0
                    ? cust.getTotalSpent()
                    : custOrders.stream().mapToDouble(CustomerOrder::getTotalAmount).sum();
            cData.put("totalSpent", Math.round(totalSpent * 100.0) / 100.0);

            // Classify Customer: Market / Business vs Person / Regular Customer
            String name = cust.getName() != null ? cust.getName().trim() : "";
            String lowerName = name.toLowerCase();
            boolean isMarket = lowerName.contains("mart") || lowerName.contains("market")
                    || lowerName.contains("superstore") || lowerName.contains("hypermarket")
                    || lowerName.contains("supercenter") || lowerName.contains("pos")
                    || lowerName.contains("electronics") || lowerName.contains("stores")
                    || lowerName.contains("traders") || lowerName.contains("solutions")
                    || lowerName.contains("logistics") || lowerName.contains("hub")
                    || lowerName.contains("supplies") || lowerName.contains("express")
                    || lowerName.contains("commercial") || lowerName.contains("company")
                    || lowerName.contains("corp") || name.contains("#");

            String customerType = isMarket ? "MARKET" : "PERSON";
            String customerTypeLabel = isMarket ? "Market / Business" : "Person / Regular Customer";
            cData.put("customerType", customerType);
            cData.put("customerTypeLabel", customerTypeLabel);

            // Tier based on Periods/Frequency & Volume ($ spent)
            String tier;
            String tierKey;
            String tierClass;
            String cadence;
            String retentionStatus;

            if (isMarket) {
                if (totalOrders >= 5 || totalSpent >= 8000.0) {
                    tier = "Market: VIP Enterprise (5+)";
                    tierKey = "MARKET_VIP";
                    tierClass = "market-vip";
                    cadence = "Weekly / High Frequency (5+ Orders)";
                    retentionStatus = "Top Commercial Account";
                } else if (totalOrders >= 3 || totalSpent >= 3500.0) {
                    tier = "Market: Frequent Wholesale (3-4)";
                    tierKey = "MARKET_FREQUENT";
                    tierClass = "market-frequent";
                    cadence = "Bi-Weekly / Regular (3-4 Orders)";
                    retentionStatus = "Consistent Wholesale Buyer";
                } else if (totalOrders == 2) {
                    tier = "Market: Standard Commercial (2)";
                    tierKey = "MARKET_RETURNING";
                    tierClass = "market-returning";
                    cadence = "Monthly / Returning (2 Orders)";
                    retentionStatus = "Active Repeat Business";
                } else {
                    tier = "Market: New Merchant (1)";
                    tierKey = "MARKET_ONE_TIME";
                    tierClass = "market-one-time";
                    cadence = "One-Time / Onboarding (1 Order)";
                    retentionStatus = "New Commercial Account";
                }
            } else {
                if (totalOrders >= 5 || totalSpent >= 3000.0) {
                    tier = "Person: VIP Regular (5+)";
                    tierKey = "PERSON_VIP";
                    tierClass = "person-vip";
                    cadence = "Frequent Repeat (5+ Orders)";
                    retentionStatus = "Loyal Personal Customer";
                } else if (totalOrders >= 3 || totalSpent >= 1500.0) {
                    tier = "Person: Regular Buyer (3-4)";
                    tierKey = "PERSON_FREQUENT";
                    tierClass = "person-frequent";
                    cadence = "Regular / Monthly (3-4 Orders)";
                    retentionStatus = "Established Individual Buyer";
                } else if (totalOrders == 2) {
                    tier = "Person: Returning Shopper (2)";
                    tierKey = "PERSON_RETURNING";
                    tierClass = "person-returning";
                    cadence = "Returning (2 Orders)";
                    retentionStatus = "Repeat Retail Customer";
                } else {
                    tier = "Person: One-Time Buyer (1)";
                    tierKey = "PERSON_ONE_TIME";
                    tierClass = "person-one-time";
                    cadence = "One-Time (1 Order)";
                    retentionStatus = "New Retail Shopper";
                }
            }

            cData.put("tier", tier);
            cData.put("tierKey", tierKey);
            cData.put("tierClass", tierClass);
            cData.put("cadence", cadence);
            cData.put("retentionStatus", retentionStatus);
            cData.put("isRepeatCustomer", totalOrders >= 2);

            int pendingFollowups = 0;
            boolean hasOverdue = false;
            for (CustomerOrder o : custOrders) {
                List<OrderFollowupCheck> oChecks = checksByOrderId.getOrDefault(o.getId(), Collections.emptyList());
                for (OrderFollowupCheck chk : oChecks) {
                    boolean isPresetActive = (chk.getPreset() != null && activePresetIds.contains(chk.getPreset().getId()))
                            || (chk.getMilestoneName() != null && activePresetNames.contains(chk.getMilestoneName().trim().toLowerCase()));
                    if (isPresetActive && !chk.isCompleted()) {
                        pendingFollowups++;
                        if ("OVERDUE".equalsIgnoreCase(chk.getStatus())) {
                            hasOverdue = true;
                        }
                    }
                }
            }
            cData.put("pendingFollowups", pendingFollowups);
            cData.put("allFollowupsCompleted", pendingFollowups == 0);
            cData.put("followupStatus", pendingFollowups == 0 ? "DONE" : (hasOverdue ? "ALERT" : "IDLE"));

            customerSummaryMap.put(cust.getId(), cData);
        }
        List<Map<String, Object>> customerSummaryList = new ArrayList<>(customerSummaryMap.values());
        customerSummaryList.sort(Comparator.comparing((Map<String, Object> c) -> (Integer) c.get("totalOrders")).reversed());

        // Assembly
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", period);
        report.put("generatedAt", Instant.now());
        report.put("summary", summary);
        report.put("loyaltyTiers", loyaltyTiers);
        report.put("milestones", milestoneList);
        report.put("satisfaction", satisfactionMap);
        report.put("satisfactionNotes", notesLedger);
        report.put("products", productList);
        report.put("customers", customerSummaryList);
        report.put("customerSummary", customerSummaryList);

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
