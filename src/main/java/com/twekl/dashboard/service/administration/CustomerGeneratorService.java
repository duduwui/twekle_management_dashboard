package com.twekl.dashboard.service.administration;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service to simulate and generate realistic daily customer intake (~40 customers/day),
 * including Individuals, Organizations, varied order counts (Loyal, Regular, One-Time),
 * realistic Iraqi demographics, products, and synchronized follow-up milestones & feedback.
 */
@Service
public class CustomerGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(CustomerGeneratorService.class);

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final TimeFilterPresetRepository presetRepository;
    private final CacheManager cacheManager;

    private static final String[] FIRST_NAMES = {
        "Ahmed", "Mohammed", "Ali", "Hassan", "Hussein", "Omar", "Mustafa", "Yousif",
        "Zaid", "Karrar", "Haider", "Bilal", "Saif", "Ameer", "Tariq", "Mahmoud",
        "Zainab", "Fatima", "Sarah", "Noor", "Maryam", "Rawan", "Huda", "Aya",
        "Dania", "Mina", "Rania", "Ban", "Shahad", "Zahra", "Reem", "Layla"
    };

    private static final String[] LAST_NAMES = {
        "Al-Saadi", "Al-Mansoor", "Al-Khafaji", "Al-Dulaimi", "Al-Bayati", "Al-Janabi",
        "Al-Jubouri", "Al-Obeidi", "Al-Hashemi", "Al-Tamimi", "Al-Rubaie", "Al-Shammari",
        "Al-Mashhadani", "Al-Qaisi", "Al-Husseini", "Al-Zubaidi", "Al-Amiri", "Al-Kinani"
    };

    private static final String[] ORG_PREFIXES = {
        "Baghdad", "Babylon", "Dijlah", "Tigris", "Erbil", "Basra", "Al-Mansour",
        "Al-Rafidain", "Al-Rasheed", "Ur", "Sumerian", "Al-Noor", "Al-Zawraa",
        "Nineveh", "Karkh", "Al-Ameen", "Al-Furat", "Mesopotamia"
    };

    private static final String[] ORG_SUFFIXES = {
        "Trading Co.", "Electronics Hub", "Retail Solutions", "Supercenter",
        "Commercial Supplies", "IT Logistics", "Medical Supplies Co.", "Hypermarket",
        "Technical Solutions", "Wholesale Stores", "Office Supplies Corp",
        "Enterprise POS", "Distribution Hub", "Digital Services", "Market Express"
    };

    private static final String[] CITIES = {
        "Baghdad", "Erbil", "Basra", "Sulaymaniyah", "Najaf", "Karbala",
        "Mosul", "Kirkuk", "Hillah", "Nasiriyah", "Duhok", "Ramadi"
    };

    private static final String[] PHONE_PREFIXES = { "0770", "0780", "0750", "0771", "0781", "0751" };

    private static final String[][] PRODUCTS = {
        // Name, Base Price
        { "ThinkPad E14 Gen 5 Laptop (i7/16GB/512GB)", "890" },
        { "Dell UltraSharp 27\" 4K USB-C Monitor", "450" },
        { "Logitech MX Master 3S Wireless Mouse", "95" },
        { "HP LaserJet Pro 4003dn Printer", "320" },
        { "Cisco CBS250 24-Port Gigabit Smart Switch", "420" },
        { "Samsung Neo QLED 55\" Smart Display", "780" },
        { "Apple MacBook Air M2 13.6\" (8-Core/256GB)", "1050" },
        { "Anker 737 PowerBank 24,000mAh 140W", "130" },
        { "Ubiquiti UniFi U6-Pro WiFi 6 AP", "180" },
        { "Sony WH-1000XM5 Noise Canceling Headphones", "340" },
        { "Zebra ZD220 Desktop Barcode Printer", "260" },
        { "Honeywell Voyager 1250g Barcode Scanner", "85" },
        { "Epson EcoTank L3250 All-in-One Printer", "195" },
        { "APC Smart-UPS 1500VA LCD 230V", "550" },
        { "Ergonomic Mesh High-Back Executive Chair", "210" },
        { "Thermal Receipt Paper Rolls (Box of 50)", "45" },
        { "Modular Heavy-Duty L-Shaped Workstation", "380" }
    };

    private static final String[] POSITIVE_NOTES = {
        "Customer confirmed receipt and is very satisfied with quality.",
        "Followed up via WhatsApp, confirmed all items delivered on time.",
        "Customer commended the fast delivery and professional packaging.",
        "Staff assisted customer with device setup. Highly satisfied.",
        "Confirmed receipt, customer stated they will order again soon."
    };

    private static final String[] NEUTRAL_NOTES = {
        "Delivery took slightly longer than expected due to courier, but received.",
        "Customer requested an extra invoice copy via email; sent successfully.",
        "Package arrived in good condition; customer is testing functionality."
    };

    private static final String[] UNSATISFIED_NOTES = {
        "Courier arrived 2 hours late. Customer expressed slight dissatisfaction.",
        "Customer asked for clarification on warranty terms."
    };

    @Autowired
    public CustomerGeneratorService(
            final CustomerRepository customerRepository,
            final CustomerOrderRepository orderRepository,
            final OrderFollowupCheckRepository followupCheckRepository,
            final CustomerFeedbackRepository feedbackRepository,
            final TimeFilterPresetRepository presetRepository,
            final CacheManager cacheManager) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.feedbackRepository = feedbackRepository;
        this.presetRepository = presetRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Daily scheduled intake running every day at 02:00 AM server time.
     * Generates ~40 realistic customers for the new day.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledDailyIntake() {
        log.info("Starting scheduled daily customer intake generator...");
        try {
            int count = ThreadLocalRandom.current().nextInt(38, 44);
            Map<String, Object> result = generateBatch(1, count, true);
            log.info("Scheduled daily intake completed successfully: {}", result);
        } catch (Exception e) {
            log.error("Error during scheduled daily customer intake", e);
        }
    }

    /**
     * Generate customers and activities for a specified number of days with target count per day.
     *
     * @param days number of days (e.g. 1 for today, or 7 for past week)
     * @param countPerDay average customers per day (~40)
     * @param seedHistory whether to seed tiered order history for loyalty tiers
     * @return Summary map with counts
     */
    @Transactional
    public Map<String, Object> generateBatch(int days, int countPerDay, boolean seedHistory) {
        int safeDays = Math.max(1, Math.min(60, days));
        int safeCount = Math.max(1, Math.min(200, countPerDay));

        List<TimeFilterPreset> activePresets = presetRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Instant now = Instant.now();

        int totalCustomersCreated = 0;
        int totalOrdersCreated = 0;
        int totalChecksCreated = 0;
        int totalFeedbacksCreated = 0;

        for (int dayOffset = safeDays - 1; dayOffset >= 0; dayOffset--) {
            // Distribute across the target day
            Instant dayBase = now.minus(dayOffset, ChronoUnit.DAYS);
            boolean isRecent = (dayOffset == 0);

            // Slight natural variance around target count (e.g. 38 to 43 for 40)
            int variance = ThreadLocalRandom.current().nextInt(-2, 3);
            int dailyCount = Math.max(1, safeCount + variance);

            for (int i = 0; i < dailyCount; i++) {
                // Determine Customer Type: 70% Individual, 30% Organization
                boolean isOrganization = ThreadLocalRandom.current().nextInt(100) < 30;
                String customerName;
                String emailDomain;

                if (isOrganization) {
                    String prefix = ORG_PREFIXES[ThreadLocalRandom.current().nextInt(ORG_PREFIXES.length)];
                    String suffix = ORG_SUFFIXES[ThreadLocalRandom.current().nextInt(ORG_SUFFIXES.length)];
                    customerName = prefix + " " + suffix;
                    emailDomain = prefix.toLowerCase().replaceAll("[^a-z0-9]", "") + ".iq";
                } else {
                    String first = FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
                    String last = LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
                    customerName = first + " " + last;
                    emailDomain = "gmail.com";
                }

                String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
                String phonePrefix = PHONE_PREFIXES[ThreadLocalRandom.current().nextInt(PHONE_PREFIXES.length)];
                String phoneNumber = phonePrefix + String.format("%07d", ThreadLocalRandom.current().nextInt(1000000, 9999999));

                String cleanName = customerName.toLowerCase().replaceAll("[^a-z0-9]", ".");
                String email = cleanName.length() > 20 ? cleanName.substring(0, 20) : cleanName;
                email = email.replaceAll("^\\.+|\\.+$", "") + (ThreadLocalRandom.current().nextInt(10, 99)) + "@" + emailDomain;

                // Stagger intake times dynamically across different hours and minutes so remaining times are diverse and unique
                int minuteOffset;
                if (dayOffset == 0) {
                    // For today: distribute across the day from 15 minutes ago up to 28 hours ago
                    // Some will have 23h left, some 18h left, some 6h left, some 45m left, some overdue
                    double ratio = (double) i / Math.max(1, dailyCount - 1);
                    int baseMinutes = (int) (15 + ratio * (28 * 60 - 30));
                    int jitter = ThreadLocalRandom.current().nextInt(-18, 19);
                    minuteOffset = Math.max(15, baseMinutes + jitter);
                } else {
                    minuteOffset = ThreadLocalRandom.current().nextInt(60, 1380);
                }
                Instant customerCreatedAt = dayBase.minus(minuteOffset, ChronoUnit.MINUTES);

                Customer customer = new Customer();
                customer.setName(customerName);
                customer.setPhoneNumber(phoneNumber);
                customer.setEmail(email);
                customer.setCity(city);
                customer.setActive(true);
                customer.setCreatedAt(customerCreatedAt);
                customer.setUpdatedAt(customerCreatedAt);

                Customer savedCustomer = customerRepository.save(customer);
                totalCustomersCreated++;

                // Determine Tier distribution:
                // 50% One-Time (1 order)
                // 30% Regular (2-3 orders)
                // 20% Loyal (4-6 orders)
                int orderCount = 1;
                int tierRoll = ThreadLocalRandom.current().nextInt(100);
                if (seedHistory) {
                    if (tierRoll < 50) {
                        orderCount = 1; // One-Time
                    } else if (tierRoll < 80) {
                        orderCount = ThreadLocalRandom.current().nextInt(2, 4); // 2-3 orders (Regular)
                    } else {
                        orderCount = ThreadLocalRandom.current().nextInt(4, 7); // 4-6 orders (Loyal)
                    }
                }

                double totalSpent = 0.0;
                Instant lastOrderDate = customerCreatedAt;

                // Create orders from oldest to newest
                for (int oIdx = 0; oIdx < orderCount; oIdx++) {
                    Instant orderDate;
                    if (oIdx == orderCount - 1) {
                        // The primary/latest order is placed on customer intake day
                        orderDate = customerCreatedAt;
                    } else {
                        // Historical orders placed earlier (30 to 180 days prior)
                        int daysPrior = (orderCount - 1 - oIdx) * ThreadLocalRandom.current().nextInt(25, 60);
                        orderDate = customerCreatedAt.minus(daysPrior, ChronoUnit.DAYS);
                    }

                    String[] product = PRODUCTS[ThreadLocalRandom.current().nextInt(PRODUCTS.length)];
                    String itemsSummary = product[0];
                    double unitPrice = Double.parseDouble(product[1]);
                    int quantity = isOrganization ? ThreadLocalRandom.current().nextInt(1, 5) : 1;
                    double orderAmount = unitPrice * quantity;
                    totalSpent += orderAmount;
                    if (orderDate.isAfter(lastOrderDate)) {
                        lastOrderDate = orderDate;
                    }

                    String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(100, 999);
                    CustomerOrder order = new CustomerOrder(orderNumber, savedCustomer, itemsSummary, quantity, orderAmount, orderDate);
                    CustomerOrder savedOrder = orderRepository.save(order);
                    totalOrdersCreated++;

                    // Generate milestone checks for this order
                    for (TimeFilterPreset preset : activePresets) {
                        OrderFollowupCheck check = new OrderFollowupCheck(savedOrder, preset);
                        check.setCreatedAt(orderDate);
                        check.setUpdatedAt(orderDate);

                        // Calculate target deadline
                        long durationHours = calculateDurationInHours(preset.getDurationValue(), preset.getDurationUnit());
                        Instant targetDeadline = orderDate.plus(durationHours, ChronoUnit.HOURS);

                        // If the milestone deadline is in the past (> 2 hours ago), complete it realistically
                        if (targetDeadline.isBefore(now.minus(2, ChronoUnit.HOURS))) {
                            check.setCompleted(true);
                            check.setStatus("COMPLETED");
                            check.setCheckedBy(isOrganization ? "Account Manager" : "Customer Support");
                            check.setCompletedDate(targetDeadline.plus(ThreadLocalRandom.current().nextInt(15, 90), ChronoUnit.MINUTES));
                            check.setCheckedAt(check.getCompletedDate());

                            // Realistic satisfaction: 80% SATISFIED, 15% NEUTRAL, 5% UNSATISFIED
                            int satRoll = ThreadLocalRandom.current().nextInt(100);
                            int rating;
                            String note;
                            if (satRoll < 80) {
                                check.setSatisfaction("SATISFIED");
                                rating = ThreadLocalRandom.current().nextInt(4, 6); // 4 or 5
                                note = POSITIVE_NOTES[ThreadLocalRandom.current().nextInt(POSITIVE_NOTES.length)];
                            } else if (satRoll < 95) {
                                check.setSatisfaction("NEUTRAL");
                                rating = 3;
                                note = NEUTRAL_NOTES[ThreadLocalRandom.current().nextInt(NEUTRAL_NOTES.length)];
                            } else {
                                check.setSatisfaction("UNSATISFIED");
                                rating = ThreadLocalRandom.current().nextInt(1, 3); // 1 or 2
                                note = UNSATISFIED_NOTES[ThreadLocalRandom.current().nextInt(UNSATISFIED_NOTES.length)];
                            }
                            check.setNotes(note);

                            // Also save CustomerFeedback record for satisfaction reporting
                            CustomerFeedback feedback = new CustomerFeedback(savedCustomer, savedOrder, rating, note);
                            feedback.setFeedbackDate(check.getCompletedDate());
                            feedbackRepository.save(feedback);
                            totalFeedbacksCreated++;

                        } else {
                            // Check is currently pending or approaching
                            check.setCompleted(false);
                            if (targetDeadline.isBefore(now)) {
                                check.setStatus("OVERDUE");
                            } else {
                                check.setStatus("PENDING");
                            }
                        }

                        followupCheckRepository.save(check);
                        totalChecksCreated++;
                    }
                }

                savedCustomer.setTotalOrders(orderCount);
                savedCustomer.setTotalSpent(totalSpent);
                savedCustomer.setLastOrderDate(lastOrderDate);
                customerRepository.save(savedCustomer);
            }
        }

        // Evict all caches so reports, dashboards, and follow-ups reflect new data instantly
        evictAllCaches();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("daysGenerated", safeDays);
        response.put("customersCreated", totalCustomersCreated);
        response.put("ordersCreated", totalOrdersCreated);
        response.put("checksCreated", totalChecksCreated);
        response.put("feedbacksCreated", totalFeedbacksCreated);
        response.put("status", "SUCCESS");
        response.put("message", "Generated " + totalCustomersCreated + " customers and " + totalOrdersCreated + " orders across " + safeDays + " days.");

        return response;
    }

    @Transactional
    public Map<String, Object> resetAndSeed(int count) {
        feedbackRepository.deleteAllInBatch();
        followupCheckRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        evictAllCaches();
        return generateBatch(1, count, true);
    }

    private long calculateDurationInHours(Integer val, String unit) {
        int v = (val != null && val > 0) ? val : 24;
        String u = (unit != null) ? unit.toUpperCase().trim() : "HOURS";
        return switch (u) {
            case "DAYS" -> v * 24L;
            case "WEEKS" -> v * 168L;
            case "MONTHS" -> v * 720L;
            default -> v;
        };
    }

    public void evictAllCaches() {
        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
    }
}
