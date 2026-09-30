package com.twekl.dashboard.config;

import com.twekl.dashboard.model.*;
import com.twekl.dashboard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;
    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    public DataInitializer(AdminRepository adminRepository, 
                           RoleRepository roleRepository,
                           TimeFilterPresetRepository timeFilterPresetRepository,
                           CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           OrderFollowupCheckRepository followupCheckRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE order_followup_checks MODIFY COLUMN image_url LONGTEXT");
            log.info("Successfully ensured order_followup_checks.image_url is LONGTEXT");
        } catch (Exception e) {
            log.warn("Notice updating table column image_url: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE order_followup_checks ADD COLUMN satisfaction VARCHAR(30) NULL");
            log.info("Successfully ensured order_followup_checks.satisfaction column exists");
        } catch (Exception e) {
            log.debug("Column satisfaction already exists in order_followup_checks");
        }

        try {
            jdbcTemplate.execute("ALTER TABLE customer_feedbacks ADD COLUMN satisfaction VARCHAR(30) NULL");
            log.info("Successfully ensured customer_feedbacks.satisfaction column exists");
        } catch (Exception e) {
            log.debug("Column satisfaction already exists in customer_feedbacks");
        }

        if (timeFilterPresetRepository.count() == 0) {
            log.info("Seeding Dynamic Time Filter Presets...");
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("last 24 hours").durationValue(24).durationUnit("HOURS").isActive(true).build());
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("last 7 days").durationValue(7).durationUnit("DAYS").isActive(true).build());
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("last 30 days").durationValue(30).durationUnit("DAYS").isActive(true).build());
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("last 2 months").durationValue(2).durationUnit("MONTHS").isActive(true).build());
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("3 Days Ago").durationValue(3).durationUnit("DAYS").isActive(true).build());
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("60 Days Review").durationValue(60).durationUnit("DAYS").isActive(true).build());
        }

        if (roleRepository.count() == 0) {
            log.info("Seeding Default Role Templates...");
            roleRepository.save(Role.builder().name("Software Engineer").canCreate(true).canRead(true).canUpdate(true).canDelete(true).build());
            roleRepository.save(Role.builder().name("Sales Representative").canCreate(true).canRead(true).canUpdate(true).canDelete(false).build());
            roleRepository.save(Role.builder().name("Product Specialist").canCreate(false).canRead(true).canUpdate(true).canDelete(false).build());
            roleRepository.save(Role.builder().name("Read-Only Auditor").canCreate(false).canRead(true).canUpdate(false).canDelete(false).build());
        }

        if (!adminRepository.existsByUsername("twekl_super_admin")) {
            log.info("Seeding Super Admin twekl_super_admin...");
            adminRepository.save(Admin.builder()
                    .username("twekl_super_admin")
                    .password(passwordEncoder.encode("Super@2026"))
                    .phoneNumber("+966 50 111 2233")
                    .isSuperAdmin(true)
                    .canCreateRoles(true)
                    .canManageUsers(true)
                    .status("ACTIVE")
                    .build());
        }

        if (customerRepository.count() == 0) {
            log.info("Seeding Rich Customer Lifecycle & Follow-up Dataset...");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            // --- PRESET IDs: 8=last 24 hours, 9=last 7 days, 10=last 30 days ---

            // ═══════════════════════════════════════════════════════════════════
            // Customer 1: VERY FRESH — Order placed 34 minutes ago → White/Idle
            //   Demonstrates: minute-level age display ("34m old"), countdown badge
            // ═══════════════════════════════════════════════════════════════════
            Customer c1 = customerRepository.save(new Customer(null,
                "Barzan Electronics", "+964 750 111 2233", "orders@barzantech.iq",
                "Erbil", 1, 850.0, now.minusMinutes(34), 0, "ACTIVE"));
            CustomerOrder o1 = orderRepository.save(new CustomerOrder(null, c1.getId(),
                "ORD-001", now.minusMinutes(34),
                "Twekl POS Touch Terminal 15.6\" + Cash Drawer", 850.0, "CASH_ON_DELIVERY", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o1.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o1.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o1.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            // ═══════════════════════════════════════════════════════════════════
            // Customer 2: FRESH — Order placed 4h 22m ago → White/Idle, countdown shows ~19h 38m
            // ═══════════════════════════════════════════════════════════════════
            Customer c2 = customerRepository.save(new Customer(null,
                "Zana Commercial Stores", "+964 750 445 6789", "orders@zanastores.iq",
                "Sulaymaniyah", 1, 1420.0, now.minusHours(4).minusMinutes(22), 0, "ACTIVE"));
            CustomerOrder o2 = orderRepository.save(new CustomerOrder(null, c2.getId(),
                "ORD-002", now.minusHours(4).minusMinutes(22),
                "Thermal Receipt Printers (x2) + Barcode Scanner", 1420.0, "BANK_TRANSFER", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o2.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o2.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o2.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            // ═══════════════════════════════════════════════════════════════════
            // Customer 3: ALERT — Order placed 26h 15m ago → Red/Alert on 24h milestone
            //   Demonstrates: ALERT status, pending follow-up, note pre-filled
            // ═══════════════════════════════════════════════════════════════════
            Customer c3 = customerRepository.save(new Customer(null,
                "Al-Mansour Supermarket", "+964 770 555 8899", "admin@almansour-market.iq",
                "Baghdad", 1, 3850.0, now.minusHours(26).minusMinutes(15), 1, "FOLLOW_UP_24H"));
            CustomerOrder o3 = orderRepository.save(new CustomerOrder(null, c3.getId(),
                "ORD-003", now.minusHours(26).minusMinutes(15),
                "Retail POS Full Bundle + Heavy Duty Cash Drawer", 3850.0, "CREDIT_CARD", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o3.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(false).note("Milestone reached (26h old). Awaiting staff call.").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o3.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o3.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            // ═══════════════════════════════════════════════════════════════════
            // Customer 4: PARTIALLY DONE — 24h checkpoint completed (Satisfied), 7d pending (due in 4d)
            //   Order: 3 days ago → Green for 24h, pending for 7d
            // ═══════════════════════════════════════════════════════════════════
            Customer c4 = customerRepository.save(new Customer(null,
                "Kurdish Gourmet Restaurant", "+964 771 333 4455", "info@kurdishgourmet.iq",
                "Duhok", 1, 1890.0, now.minusDays(3).minusHours(2), 3, "ACTIVE"));
            CustomerOrder o4 = orderRepository.save(new CustomerOrder(null, c4.getId(),
                "ORD-004", now.minusDays(3).minusHours(2),
                "Kitchen Order Display KDS-10 + Cloud Subscription", 1890.0, "CREDIT_CARD", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o4.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(true)
                .note("Kitchen display installed and staff trained on order routing. All good.")
                .satisfaction("SATISFIED").checkedBy("Agent Sarah")
                .checkedAt(now.minusDays(2).minusHours(1)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o4.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o4.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            // ═══════════════════════════════════════════════════════════════════
            // Customer 5: ALL DONE — 8 days ago, 24h + 7d completed (Satisfied both)
            //   Demonstrates: Green/Done status, completed notes with satisfaction
            // ═══════════════════════════════════════════════════════════════════
            Customer c5 = customerRepository.save(new Customer(null,
                "Baban Tech Retail Group", "+964 750 998 1122", "tech@babangroup.com",
                "Erbil", 1, 4150.0, now.minusDays(8).minusHours(3), 8, "FOLLOWED_UP"));
            CustomerOrder o5 = orderRepository.save(new CustomerOrder(null, c5.getId(),
                "ORD-005", now.minusDays(8).minusHours(3),
                "Touchscreen POS Display + Dual Cash Drawers", 4150.0, "BANK_TRANSFER", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o5.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(true)
                .note("Initial deployment verified. Hardware online. Store manager confirmed all terminals working.")
                .satisfaction("SATISFIED").checkedBy("Agent Tariq")
                .checkedAt(now.minusDays(7).minusHours(2)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o5.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(true)
                .note("1-week milestone audit completed successfully. All 4 POS stations fully operational.")
                .satisfaction("SATISFIED").checkedBy("Agent Tariq")
                .checkedAt(now.minusDays(1).minusHours(5)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o5.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            // ═══════════════════════════════════════════════════════════════════
            // Customer 6: MULTI-ORDER — Old order (9d, all done) + New order (7h, fresh idle)
            //   Demonstrates: multiple orders on same customer; new one is white/idle
            // ═══════════════════════════════════════════════════════════════════
            Customer c6 = customerRepository.save(new Customer(null,
                "Rayan Pharmacy Chain", "+964 750 333 5522", "contact@rayanpharm.iq",
                "Erbil", 2, 5600.0, now.minusHours(7).minusMinutes(8), 0, "ACTIVE"));
            // Old completed order
            CustomerOrder o6a = orderRepository.save(new CustomerOrder(null, c6.getId(),
                "ORD-006A", now.minusDays(9).minusHours(4),
                "Pharmacy Barcode Matrix Scanner + Label Printer", 3200.0, "BANK_TRANSFER", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o6a.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(true)
                .note("Scanner operational at all 3 checkout counters.")
                .satisfaction("SATISFIED").checkedBy("Agent Tariq")
                .checkedAt(now.minusDays(8).minusHours(3)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o6a.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(true)
                .note("Client requested Kurdish language receipt templates. Will follow up with dev team.")
                .satisfaction("NEUTRAL").checkedBy("Agent Tariq")
                .checkedAt(now.minusDays(2).minusHours(1)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o6a.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());
            // New fresh order (7h 8m old)
            CustomerOrder o6b = orderRepository.save(new CustomerOrder(null, c6.getId(),
                "ORD-006B", now.minusHours(7).minusMinutes(8),
                "Wireless Handheld Scanners (x3)", 2400.0, "CASH_ON_DELIVERY", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o6b.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o6b.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o6b.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            // ═══════════════════════════════════════════════════════════════════
            // Customer 7: UNSATISFIED → RESOLVED — 12d order, initial issue, then resolved
            //   Demonstrates: full lifecycle with negative then positive resolution
            // ═══════════════════════════════════════════════════════════════════
            Customer c7 = customerRepository.save(new Customer(null,
                "Erbil Modern Bakery", "+964 750 777 6655", "bakery@erbilmodern.iq",
                "Erbil", 1, 740.0, now.minusDays(12).minusHours(5), 12, "FOLLOWED_UP"));
            CustomerOrder o7 = orderRepository.save(new CustomerOrder(null, c7.getId(),
                "ORD-007", now.minusDays(12).minusHours(5),
                "High-Speed Kitchen Ticket Printer (x2)", 740.0, "CASH_ON_DELIVERY", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o7.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(true)
                .note("Bluetooth sync issue occurred during rush hour — printers dropped connection.")
                .satisfaction("UNSATISFIED").checkedBy("Agent Sarah")
                .checkedAt(now.minusDays(11).minusHours(4)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o7.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(true)
                .note("Replaced bluetooth adapter with direct ethernet. Ran stress test during dinner rush — zero drops. Customer very pleased with swift response.")
                .satisfaction("SATISFIED").checkedBy("Agent Sarah")
                .checkedAt(now.minusDays(5).minusHours(2)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o7.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            // ═══════════════════════════════════════════════════════════════════
            // Customer 8: ALERT (near-miss) — Order placed exactly 24h 3m ago → Red on 24h filter
            //   Demonstrates: very recently triggered alert, tiny time overage
            // ═══════════════════════════════════════════════════════════════════
            Customer c8 = customerRepository.save(new Customer(null,
                "Suli City Cafe", "+964 770 222 9988", "manager@sulicafe.iq",
                "Sulaymaniyah", 1, 620.0, now.minusHours(24).minusMinutes(3), 1, "FOLLOW_UP_24H"));
            CustomerOrder o8 = orderRepository.save(new CustomerOrder(null, c8.getId(),
                "ORD-008", now.minusHours(24).minusMinutes(3),
                "Cloud POS Tablet Stand + Receipt Printer", 620.0, "CASH_ON_DELIVERY", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o8.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o8.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(false).note("").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o8.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            // ═══════════════════════════════════════════════════════════════════
            // Customer 9: NEUTRAL — 10 days, 24h done (satisfied), 7d done (neutral / language issue pending)
            // ═══════════════════════════════════════════════════════════════════
            Customer c9 = customerRepository.save(new Customer(null,
                "Kirkuk Medical Supplies", "+964 750 444 3377", "purchase@kirkukmed.iq",
                "Kirkuk", 1, 980.0, now.minusDays(10).minusHours(1), 10, "FOLLOWED_UP"));
            CustomerOrder o9 = orderRepository.save(new CustomerOrder(null, c9.getId(),
                "ORD-009", now.minusDays(10).minusHours(1),
                "Inventory Barcode Scanner Pro + Label Printer", 980.0, "BANK_TRANSFER", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o9.getId()).presetId(8L)
                .presetName("last 24 hours").durationValue(24).durationUnit("HOURS")
                .isCompleted(true)
                .note("Scanner setup and tested successfully at warehouse entry point.")
                .satisfaction("SATISFIED").checkedBy("Agent Tariq")
                .checkedAt(now.minusDays(9).minusHours(1)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o9.getId()).presetId(9L)
                .presetName("last 7 days").durationValue(7).durationUnit("DAYS")
                .isCompleted(true)
                .note("Software working but staff prefer Arabic interface. Forwarded feature request.")
                .satisfaction("NEUTRAL").checkedBy("Agent Tariq")
                .checkedAt(now.minusDays(3).minusHours(2)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o9.getId()).presetId(10L)
                .presetName("last 30 days").durationValue(30).durationUnit("DAYS")
                .isCompleted(false).note("").build());

            log.info("Seeded 9 customers, 10 orders, 27 checkpoints — full lifecycle: fresh, alert, done, multi-order, issue-resolved.");
        }

        log.info("System initialization complete. Super admin, role templates, time filters, and customer lifecycle dataset active.");
    }
}
