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
            log.info("Seeding Realistic Customer Lifecycle and Follow-up Demonstration Dataset...");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            // Customer 1: Fresh Customer (Order placed 3 hours ago -> Age = 3h < 24h -> White / Idle)
            Customer c1 = customerRepository.save(new Customer(null, "Rebwar Barzan (Fresh Client)", "+964 750 123 4567", "rebwar.barzan@example.com", "Erbil", 1, 650.0, now.minusHours(3), 0, "ACTIVE"));
            CustomerOrder o101 = orderRepository.save(new CustomerOrder(null, c1.getId(), "ORD-101", now.minusHours(3), "Twekl POS Touch Terminal 15.6\"", 650.0, "CASH_ON_DELIVERY", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o101.getId()).presetId(1L).presetName("last 24 hours").durationValue(24).durationUnit("HOURS").isCompleted(false).note("Scheduled checkpoint (Order placed 3h ago, due in 21h)").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o101.getId()).presetId(2L).presetName("last 7 days").durationValue(7).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in ~7 days)").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o101.getId()).presetId(3L).presetName("last 30 days").durationValue(30).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in ~30 days)").build());

            // Customer 2: Pending Follow-up (Order placed 26 hours ago -> Age = 26h > 24h -> Red / Alert)
            Customer c2 = customerRepository.save(new Customer(null, "Zana Commercial Stores", "+964 750 445 6789", "orders@zanastores.iq", "Sulaymaniyah", 1, 1420.0, now.minusHours(26), 1, "FOLLOW_UP_24H"));
            CustomerOrder o102 = orderRepository.save(new CustomerOrder(null, c2.getId(), "ORD-102", now.minusHours(26), "Thermal Receipt Printers (x2) + Barcode Scanner", 1420.0, "BANK_TRANSFER", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o102.getId()).presetId(1L).presetName("last 24 hours").durationValue(24).durationUnit("HOURS").isCompleted(false).note("Milestone reached (26h old). Awaiting staff follow-up call.").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o102.getId()).presetId(2L).presetName("last 7 days").durationValue(7).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in 6 days)").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o102.getId()).presetId(3L).presetName("last 30 days").durationValue(30).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in 29 days)").build());

            // Customer 3: Fully Followed Up (Order placed 8 days ago -> 24h & 7d completed -> Green / All Done)
            Customer c3 = customerRepository.save(new Customer(null, "Al-Mansour Supermarket", "+964 770 555 8899", "admin@almansour-market.iq", "Baghdad", 1, 3850.0, now.minusDays(8), 8, "ACTIVE"));
            CustomerOrder o103 = orderRepository.save(new CustomerOrder(null, c3.getId(), "ORD-103", now.minusDays(8), "Retail POS Full Bundle + Heavy Duty Cash Drawer", 3850.0, "CREDIT_CARD", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o103.getId()).presetId(1L).presetName("last 24 hours").durationValue(24).durationUnit("HOURS").isCompleted(true).note("Called store manager. Terminal set up smoothly and operational.").satisfaction("SATISFIED").checkedBy("Agent Sarah").checkedAt(now.minusDays(7)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o103.getId()).presetId(2L).presetName("last 7 days").durationValue(7).durationUnit("DAYS").isCompleted(true).note("Customer very satisfied with hardware speed and reliability.").satisfaction("SATISFIED").checkedBy("Agent Sarah").checkedAt(now.minusDays(1)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o103.getId()).presetId(3L).presetName("last 30 days").durationValue(30).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in 22 days)").build());

            // Customer 4: Multi-Orders (Order A placed 9 days ago is Green; Order B placed 6 hours ago is White)
            Customer c4 = customerRepository.save(new Customer(null, "Baban Tech Retail Group", "+964 750 998 1122", "tech@babangroup.com", "Erbil", 2, 4150.0, now.minusHours(6), 0, "ACTIVE"));
            CustomerOrder o104A = orderRepository.save(new CustomerOrder(null, c4.getId(), "ORD-104A", now.minusDays(9), "Touchscreen POS Display + Dual Cash Drawers", 3200.0, "BANK_TRANSFER", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o104A.getId()).presetId(1L).presetName("last 24 hours").durationValue(24).durationUnit("HOURS").isCompleted(true).note("Initial deployment verified. Hardware online.").satisfaction("SATISFIED").checkedBy("Agent Tariq").checkedAt(now.minusDays(8)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o104A.getId()).presetId(2L).presetName("last 7 days").durationValue(7).durationUnit("DAYS").isCompleted(true).note("1-week milestone audit completed successfully.").satisfaction("SATISFIED").checkedBy("Agent Tariq").checkedAt(now.minusDays(2)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o104A.getId()).presetId(3L).presetName("last 30 days").durationValue(30).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in 21 days)").build());

            CustomerOrder o104B = orderRepository.save(new CustomerOrder(null, c4.getId(), "ORD-104B", now.minusHours(6), "Wireless Handheld Scanners (x3)", 950.0, "CASH_ON_DELIVERY", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o104B.getId()).presetId(1L).presetName("last 24 hours").durationValue(24).durationUnit("HOURS").isCompleted(false).note("Scheduled checkpoint (6h old, due in 18h)").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o104B.getId()).presetId(2L).presetName("last 7 days").durationValue(7).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in ~7 days)").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o104B.getId()).presetId(3L).presetName("last 30 days").durationValue(30).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in ~30 days)").build());

            // Customer 5: Order 3 days ago (24h completed, 7d not due yet -> Ready for 2-day filter demo)
            Customer c5 = customerRepository.save(new Customer(null, "Kurdish Gourmet Restaurant", "+964 771 333 4455", "info@kurdishgourmet.iq", "Dohuk", 1, 1890.0, now.minusDays(3), 3, "ACTIVE"));
            CustomerOrder o105 = orderRepository.save(new CustomerOrder(null, c5.getId(), "ORD-105", now.minusDays(3), "Kitchen Order Display KDS-10 + Cloud Subscription", 1890.0, "CREDIT_CARD", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o105.getId()).presetId(1L).presetName("last 24 hours").durationValue(24).durationUnit("HOURS").isCompleted(true).note("Kitchen display installed and staff trained on order routing.").satisfaction("SATISFIED").checkedBy("Agent Sarah").checkedAt(now.minusDays(2)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o105.getId()).presetId(2L).presetName("last 7 days").durationValue(7).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (Order is 3 days old, due in 4 days)").build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o105.getId()).presetId(3L).presetName("last 30 days").durationValue(30).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in 27 days)").build());

            // Customer 6: Neutral Feedback (10 days old)
            Customer c6 = customerRepository.save(new Customer(null, "Rayan Pharmacy Chain", "+964 750 333 5522", "contact@rayanpharm.iq", "Erbil", 1, 980.0, now.minusDays(10), 10, "ACTIVE"));
            CustomerOrder o106 = orderRepository.save(new CustomerOrder(null, c6.getId(), "ORD-106", now.minusDays(10), "Pharmacy Barcode Matrix Scanner", 980.0, "BANK_TRANSFER", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o106.getId()).presetId(1L).presetName("last 24 hours").durationValue(24).durationUnit("HOURS").isCompleted(true).note("Scanner operational at checkout counter.").satisfaction("SATISFIED").checkedBy("Agent Tariq").checkedAt(now.minusDays(9)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o106.getId()).presetId(2L).presetName("last 7 days").durationValue(7).durationUnit("DAYS").isCompleted(true).note("Client requested Kurdish language receipt templates.").satisfaction("NEUTRAL").checkedBy("Agent Tariq").checkedAt(now.minusDays(3)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o106.getId()).presetId(3L).presetName("last 30 days").durationValue(30).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in 20 days)").build());

            // Customer 7: Unsatisfied Resolved (12 days old)
            Customer c7 = customerRepository.save(new Customer(null, "Erbil Modern Bakery", "+964 750 777 6655", "bakery@erbilmodern.iq", "Erbil", 1, 740.0, now.minusDays(12), 12, "ACTIVE"));
            CustomerOrder o107 = orderRepository.save(new CustomerOrder(null, c7.getId(), "ORD-107", now.minusDays(12), "High-Speed Kitchen Ticket Printer", 740.0, "CASH_ON_DELIVERY", "DELIVERED"));
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o107.getId()).presetId(1L).presetName("last 24 hours").durationValue(24).durationUnit("HOURS").isCompleted(true).note("Bluetooth sync issue occurred during rush hour.").satisfaction("UNSATISFIED").checkedBy("Agent Sarah").checkedAt(now.minusDays(11)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o107.getId()).presetId(2L).presetName("last 7 days").durationValue(7).durationUnit("DAYS").isCompleted(true).note("Replaced bluetooth adapter with direct ethernet cable. Customer very pleased with swift support.").satisfaction("SATISFIED").checkedBy("Agent Sarah").checkedAt(now.minusDays(5)).build());
            followupCheckRepository.save(OrderFollowupCheck.builder().orderId(o107.getId()).presetId(3L).presetName("last 30 days").durationValue(30).durationUnit("DAYS").isCompleted(false).note("Scheduled checkpoint (due in 18 days)").build());

            log.info("Successfully seeded dynamic customer follow-up dataset (7 customers, 8 orders, 24 milestones).");
        }

        log.info("System initialization complete. Super admin, role templates, time filters, and customer lifecycle dataset active.");
    }
}
