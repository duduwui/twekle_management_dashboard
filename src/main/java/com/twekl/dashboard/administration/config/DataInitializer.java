package com.twekl.dashboard.administration.config;

import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.model.customer.Customer;
import com.twekl.dashboard.administration.model.order.CustomerFeedback;
import com.twekl.dashboard.administration.model.order.CustomerOrder;
import com.twekl.dashboard.administration.model.order.OrderFollowupCheck;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import com.twekl.dashboard.administration.repository.admin.AdminRepository;
import com.twekl.dashboard.administration.repository.customer.CustomerRepository;
import com.twekl.dashboard.administration.repository.order.CustomerFeedbackRepository;
import com.twekl.dashboard.administration.repository.order.CustomerOrderRepository;
import com.twekl.dashboard.administration.repository.order.OrderFollowupCheckRepository;
import com.twekl.dashboard.administration.repository.role.RoleRepository;
import com.twekl.dashboard.administration.repository.timefilter.TimeFilterPresetRepository;
import com.twekl.dashboard.administration.service.admin.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DataInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;
    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;

    public DataInitializer(AdminRepository adminRepository, 
                           RoleRepository roleRepository,
                           TimeFilterPresetRepository timeFilterPresetRepository,
                           CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           CustomerFeedbackRepository feedbackRepository,
                           OrderFollowupCheckRepository followupCheckRepository,
                           PasswordEncoder passwordEncoder,
                           AdminService adminService) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminService = adminService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!isInitialized.compareAndSet(false, true)) {
            return;
        }
        initializeData();
    }

    public void initializeData() {
        if (timeFilterPresetRepository.count() == 0) {
            log.info("Seeding Dynamic Time Filter Presets...");
            timeFilterPresetRepository.save(new TimeFilterPreset("24_HOURS", "Last 24 Hours", "آخر 24 ساعة", "٢٤ کاتژمێری ڕابردوو", 24, "HOURS", 1));
            timeFilterPresetRepository.save(new TimeFilterPreset("3_DAYS", "3 Days Ago", "منذ 3 أيام", "٣ ڕۆژ لەمەوبەر", 3, "DAYS", 2));
            timeFilterPresetRepository.save(new TimeFilterPreset("7_DAYS", "Last 7 Days", "آخر 7 أيام", "٧ ڕۆژی ڕابردوو", 7, "DAYS", 3));
            timeFilterPresetRepository.save(new TimeFilterPreset("30_DAYS", "Last 30 Days", "آخر 30 يوم", "٣٠ ڕۆژی ڕابردوو", 30, "DAYS", 4));
            timeFilterPresetRepository.save(new TimeFilterPreset("2_MONTHS", "Last 2 Months", "آخر شهرين", "٢ مانگی ڕابردوو", 2, "MONTHS", 5));
            timeFilterPresetRepository.save(new TimeFilterPreset("60_DAYS", "60 Days Review", "مراجعة 60 يوم", "پێداچوونەوەی ٦٠ ڕۆژ", 60, "DAYS", 6));
        }

        Role roleEngineer = null;

        if (!roleRepository.existsByName("Software Engineer")) roleRepository.save(new Role("Software Engineer", true, true, true, true));
        if (!roleRepository.existsByName("Sales Representative")) roleRepository.save(new Role("Sales Representative", true, true, true, false));
        if (!roleRepository.existsByName("Product Specialist")) roleRepository.save(new Role("Product Specialist", false, true, true, false));
        if (!roleRepository.existsByName("Read-Only Auditor")) roleRepository.save(new Role("Read-Only Auditor", false, true, false, false));
        if (!roleRepository.existsByName("Customer Success Lead")) roleRepository.save(new Role("Customer Success Lead", true, true, true, false));
        if (!roleRepository.existsByName("Inventory Controller")) roleRepository.save(new Role("Inventory Controller", false, true, true, false));

        roleEngineer = roleRepository.findByName("Software Engineer").orElse(null);

        if (!adminRepository.existsByUsername("twekl_super_admin")) {
            log.info("Seeding Super Admin twekl_super_admin...");
            Admin superAdmin = Admin.builder()
                    .username("twekl_super_admin")
                    .password(passwordEncoder.encode("Super@2026"))
                    .phoneNumber("+966 50 111 2233")
                    .isSuperAdmin(true)
                    .canCreateRoles(true)
                    .canManageUsers(true)
                    .active(true)
                    .build();

            if (roleEngineer != null) {
                Set<Role> roles = new HashSet<>();
                roles.add(roleEngineer);
                superAdmin.setRoles(roles);
            }
            adminService.seedDefaultPermissions(superAdmin);
            adminRepository.save(superAdmin);
        }

        // Seed initial customer dataset only if empty
        if (customerRepository.count() == 0) {
            log.info("Seeding Rich Customer Lifecycle Practice Dataset with Instant timestamps...");
            Instant now = Instant.now();

            feedbackRepository.deleteAll();
            followupCheckRepository.deleteAll();
            orderRepository.deleteAll();
            customerRepository.deleteAll();

            // 1. Customer: Zana Commercial Stores (ALERT - Overdue 24h check)
            Customer c1 = new Customer(null, "Zana Commercial Stores", "+964 750 445 6789", "orders@zanastores.iq", "Sulaymaniyah");
            c1.setTotalOrders(1);
            c1.setTotalSpent(1420.0);
            c1.setLastOrderDate(now.minus(28, ChronoUnit.HOURS));
            c1 = customerRepository.save(c1);

            CustomerOrder o1 = new CustomerOrder("ORD-101", c1, "Thermal Receipt Printers (x2) + Barcode Scanner", 2, 1420.0, now.minus(28, ChronoUnit.HOURS));
            o1 = orderRepository.save(o1);

            OrderFollowupCheck ch1a = new OrderFollowupCheck(o1, "24_HOURS", "Last 24 Hours", 1);
            ch1a.setDurationValue(24);
            ch1a.setDurationUnit("HOURS");
            ch1a.setTargetDays(1);
            ch1a.setStatus("PENDING");
            ch1a.setCompleted(false);

            OrderFollowupCheck ch1_3d = new OrderFollowupCheck(o1, "3_DAYS", "3 Days Ago", 3);
            ch1_3d.setDurationValue(3);
            ch1_3d.setDurationUnit("DAYS");
            ch1_3d.setTargetDays(3);
            ch1_3d.setStatus("PENDING");
            ch1_3d.setCompleted(false);

            OrderFollowupCheck ch1b = new OrderFollowupCheck(o1, "7_DAYS", "Last 7 Days", 7);
            ch1b.setDurationValue(7);
            ch1b.setDurationUnit("DAYS");
            ch1b.setTargetDays(7);
            ch1b.setStatus("PENDING");
            ch1b.setCompleted(false);

            OrderFollowupCheck ch1c = new OrderFollowupCheck(o1, "30_DAYS", "Last 30 Days", 30);
            ch1c.setDurationValue(30);
            ch1c.setDurationUnit("DAYS");
            ch1c.setTargetDays(30);
            ch1c.setStatus("PENDING");
            ch1c.setCompleted(false);

            followupCheckRepository.save(ch1a);
            followupCheckRepository.save(ch1_3d);
            followupCheckRepository.save(ch1b);
            followupCheckRepository.save(ch1c);

            // 2. Customer: Barzan Electronics (IDLE - Brand new order, due in ~21h)
            Customer c2 = new Customer(null, "Barzan Electronics", "+964 750 111 2233", "orders@barzantech.iq", "Erbil");
            c2.setTotalOrders(1);
            c2.setTotalSpent(850.0);
            c2.setLastOrderDate(now.minus(3, ChronoUnit.HOURS));
            c2 = customerRepository.save(c2);

            CustomerOrder o2 = new CustomerOrder("ORD-102", c2, "Twekl POS Touch Terminal 15.6\" + Cash Drawer", 1, 850.0, now.minus(3, ChronoUnit.HOURS));
            o2 = orderRepository.save(o2);

            OrderFollowupCheck ch2a = new OrderFollowupCheck(o2, "24_HOURS", "Last 24 Hours", 1);
            ch2a.setDurationValue(24);
            ch2a.setDurationUnit("HOURS");
            ch2a.setTargetDays(1);
            ch2a.setStatus("PENDING");
            ch2a.setCompleted(false);

            OrderFollowupCheck ch2_3d = new OrderFollowupCheck(o2, "3_DAYS", "3 Days Ago", 3);
            ch2_3d.setDurationValue(3);
            ch2_3d.setDurationUnit("DAYS");
            ch2_3d.setTargetDays(3);
            ch2_3d.setStatus("PENDING");
            ch2_3d.setCompleted(false);

            OrderFollowupCheck ch2b = new OrderFollowupCheck(o2, "7_DAYS", "Last 7 Days", 7);
            ch2b.setDurationValue(7);
            ch2b.setDurationUnit("DAYS");
            ch2b.setTargetDays(7);
            ch2b.setStatus("PENDING");
            ch2b.setCompleted(false);

            OrderFollowupCheck ch2c = new OrderFollowupCheck(o2, "30_DAYS", "Last 30 Days", 30);
            ch2c.setDurationValue(30);
            ch2c.setDurationUnit("DAYS");
            ch2c.setTargetDays(30);
            ch2c.setStatus("PENDING");
            ch2c.setCompleted(false);

            followupCheckRepository.save(ch2a);
            followupCheckRepository.save(ch2_3d);
            followupCheckRepository.save(ch2b);
            followupCheckRepository.save(ch2c);

            // 3. Customer: Al-Mansour Supermarket (DONE - All milestones checked & verified)
            Customer c3 = new Customer(null, "Al-Mansour Supermarket", "+964 770 555 8899", "admin@almansour-market.iq", "Baghdad");
            c3.setTotalOrders(1);
            c3.setTotalSpent(3850.0);
            c3.setLastOrderDate(now.minus(35, ChronoUnit.DAYS));
            c3 = customerRepository.save(c3);

            CustomerOrder o3 = new CustomerOrder("ORD-103", c3, "Retail POS Full Bundle + Heavy Duty Cash Drawer", 1, 3850.0, now.minus(35, ChronoUnit.DAYS));
            o3 = orderRepository.save(o3);

            OrderFollowupCheck ch3a = new OrderFollowupCheck(o3, "24_HOURS", "Last 24 Hours", 1);
            ch3a.setDurationValue(24);
            ch3a.setDurationUnit("HOURS");
            ch3a.setTargetDays(1);
            ch3a.setCompleted(true);
            ch3a.setStatus("COMPLETED");
            ch3a.setCompletedDate(now.minus(34, ChronoUnit.DAYS));
            ch3a.setCheckedBy("Sara Hassan");
            ch3a.setSatisfaction("5");
            ch3a.setNotes("Initial setup and calibration verified with store manager.");

            OrderFollowupCheck ch3_3d = new OrderFollowupCheck(o3, "3_DAYS", "3 Days Ago", 3);
            ch3_3d.setDurationValue(3);
            ch3_3d.setDurationUnit("DAYS");
            ch3_3d.setTargetDays(3);
            ch3_3d.setCompleted(true);
            ch3_3d.setStatus("COMPLETED");
            ch3_3d.setCompletedDate(now.minus(32, ChronoUnit.DAYS));
            ch3_3d.setCheckedBy("Sara Hassan");
            ch3_3d.setSatisfaction("5");
            ch3_3d.setNotes("Setup verified.");

            OrderFollowupCheck ch3b = new OrderFollowupCheck(o3, "7_DAYS", "Last 7 Days", 7);
            ch3b.setDurationValue(7);
            ch3b.setDurationUnit("DAYS");
            ch3b.setTargetDays(7);
            ch3b.setCompleted(true);
            ch3b.setStatus("COMPLETED");
            ch3b.setCompletedDate(now.minus(28, ChronoUnit.DAYS));
            ch3b.setCheckedBy("Ahmed Al-Iraqi");
            ch3b.setSatisfaction("5");
            ch3b.setNotes("Cashiers trained successfully; receipt printing working smoothly.");

            OrderFollowupCheck ch3c = new OrderFollowupCheck(o3, "30_DAYS", "Last 30 Days", 30);
            ch3c.setDurationValue(30);
            ch3c.setDurationUnit("DAYS");
            ch3c.setTargetDays(30);
            ch3c.setCompleted(true);
            ch3c.setStatus("COMPLETED");
            ch3c.setCompletedDate(now.minus(5, ChronoUnit.DAYS));
            ch3c.setCheckedBy("Kawa Barzani");
            ch3c.setSatisfaction("5");
            ch3c.setNotes("Monthly review complete. Client requested quote for 2 extra branches.");

            followupCheckRepository.save(ch3a);
            followupCheckRepository.save(ch3_3d);
            followupCheckRepository.save(ch3b);
            followupCheckRepository.save(ch3c);

            CustomerFeedback f1 = new CustomerFeedback(c3, o3, 5, "POS terminal setup was very smooth and fast. Great customer support in Baghdad!");
            feedbackRepository.save(f1);

            // 4. Customer: Hawler Pharmacy Chain (ALERT - 24h done, 7 Days overdue)
            Customer c4 = new Customer(null, "Hawler Pharmacy Chain", "+964 750 789 0123", "contact@hawlerpharmacy.iq", "Erbil");
            c4.setTotalOrders(1);
            c4.setTotalSpent(1250.0);
            c4.setLastOrderDate(now.minus(9, ChronoUnit.DAYS));
            c4 = customerRepository.save(c4);

            CustomerOrder o4 = new CustomerOrder("ORD-104", c4, "Medicinal Barcode Readers & POS Hub", 1, 1250.0, now.minus(9, ChronoUnit.DAYS));
            o4 = orderRepository.save(o4);

            OrderFollowupCheck ch4a = new OrderFollowupCheck(o4, "24_HOURS", "Last 24 Hours", 1);
            ch4a.setDurationValue(24);
            ch4a.setDurationUnit("HOURS");
            ch4a.setTargetDays(1);
            ch4a.setCompleted(true);
            ch4a.setStatus("COMPLETED");
            ch4a.setCompletedDate(now.minus(8, ChronoUnit.DAYS));
            ch4a.setCheckedBy("Ahmed Al-Iraqi");
            ch4a.setSatisfaction("4");
            ch4a.setNotes("Delivery confirmed. Device online.");

            OrderFollowupCheck ch4_3d = new OrderFollowupCheck(o4, "3_DAYS", "3 Days Ago", 3);
            ch4_3d.setDurationValue(3);
            ch4_3d.setDurationUnit("DAYS");
            ch4_3d.setTargetDays(3);
            ch4_3d.setCompleted(true);
            ch4_3d.setStatus("COMPLETED");
            ch4_3d.setCompletedDate(now.minus(6, ChronoUnit.DAYS));
            ch4_3d.setCheckedBy("Ahmed Al-Iraqi");
            ch4_3d.setSatisfaction("4");
            ch4_3d.setNotes("Initial 3-day verification done.");

            OrderFollowupCheck ch4b = new OrderFollowupCheck(o4, "7_DAYS", "Last 7 Days", 7);
            ch4b.setDurationValue(7);
            ch4b.setDurationUnit("DAYS");
            ch4b.setTargetDays(7);
            ch4b.setStatus("PENDING");
            ch4b.setCompleted(false);

            OrderFollowupCheck ch4c = new OrderFollowupCheck(o4, "30_DAYS", "Last 30 Days", 30);
            ch4c.setDurationValue(30);
            ch4c.setDurationUnit("DAYS");
            ch4c.setTargetDays(30);
            ch4c.setStatus("PENDING");
            ch4c.setCompleted(false);

            followupCheckRepository.save(ch4a);
            followupCheckRepository.save(ch4_3d);
            followupCheckRepository.save(ch4b);
            followupCheckRepository.save(ch4c);

            // 5. Customer: Korek Telecom Center (IDLE - 24h done, 7 Days due in 3 days)
            Customer c5 = new Customer(null, "Korek Telecom Center", "+964 751 333 4455", "support@korekcenter.iq", "Duhok");
            c5.setTotalOrders(1);
            c5.setTotalSpent(670.0);
            c5.setLastOrderDate(now.minus(4, ChronoUnit.DAYS));
            c5 = customerRepository.save(c5);

            CustomerOrder o5 = new CustomerOrder("ORD-105", c5, "Customer Queue Screen & Mini Kiosk", 1, 670.0, now.minus(4, ChronoUnit.DAYS));
            o5 = orderRepository.save(o5);

            OrderFollowupCheck ch5a = new OrderFollowupCheck(o5, "24_HOURS", "Last 24 Hours", 1);
            ch5a.setDurationValue(24);
            ch5a.setDurationUnit("HOURS");
            ch5a.setTargetDays(1);
            ch5a.setCompleted(true);
            ch5a.setStatus("COMPLETED");
            ch5a.setCompletedDate(now.minus(3, ChronoUnit.DAYS));
            ch5a.setCheckedBy("Sara Hassan");
            ch5a.setSatisfaction("5");
            ch5a.setNotes("Kiosk operational in main Duhok showroom.");

            OrderFollowupCheck ch5_3d = new OrderFollowupCheck(o5, "3_DAYS", "3 Days Ago", 3);
            ch5_3d.setDurationValue(3);
            ch5_3d.setDurationUnit("DAYS");
            ch5_3d.setTargetDays(3);
            ch5_3d.setCompleted(true);
            ch5_3d.setStatus("COMPLETED");
            ch5_3d.setCompletedDate(now.minus(1, ChronoUnit.DAYS));
            ch5_3d.setCheckedBy("Sara Hassan");
            ch5_3d.setSatisfaction("5");
            ch5_3d.setNotes("3-day check passed.");

            OrderFollowupCheck ch5b = new OrderFollowupCheck(o5, "7_DAYS", "Last 7 Days", 7);
            ch5b.setDurationValue(7);
            ch5b.setDurationUnit("DAYS");
            ch5b.setTargetDays(7);
            ch5b.setStatus("PENDING");
            ch5b.setCompleted(false);

            OrderFollowupCheck ch5c = new OrderFollowupCheck(o5, "30_DAYS", "Last 30 Days", 30);
            ch5c.setDurationValue(30);
            ch5c.setDurationUnit("DAYS");
            ch5c.setTargetDays(30);
            ch5c.setStatus("PENDING");
            ch5c.setCompleted(false);

            followupCheckRepository.save(ch5a);
            followupCheckRepository.save(ch5_3d);
            followupCheckRepository.save(ch5b);
            followupCheckRepository.save(ch5c);

            // 6. Customer: Babylon Restaurant Group (DONE - Multi-order VIP)
            Customer c6 = new Customer(null, "Babylon Restaurant Group", "+964 771 888 9900", "management@babylonrest.iq", "Baghdad");
            c6.setTotalOrders(2);
            c6.setTotalSpent(2930.0);
            c6.setLastOrderDate(now.minus(12, ChronoUnit.DAYS));
            c6 = customerRepository.save(c6);

            CustomerOrder o6a = new CustomerOrder("ORD-106", c6, "Kitchen Display System (KDS) + 3x Heavy Printers", 1, 2150.0, now.minus(45, ChronoUnit.DAYS));
            o6a = orderRepository.save(o6a);

            OrderFollowupCheck ch6a1 = new OrderFollowupCheck(o6a, "24_HOURS", "Last 24 Hours", 1);
            ch6a1.setCompleted(true);
            ch6a1.setStatus("COMPLETED");
            ch6a1.setCompletedDate(now.minus(44, ChronoUnit.DAYS));
            ch6a1.setNotes("Kitchen staff trained on KDS display.");

            OrderFollowupCheck ch6a2 = new OrderFollowupCheck(o6a, "7_DAYS", "Last 7 Days", 7);
            ch6a2.setCompleted(true);
            ch6a2.setStatus("COMPLETED");
            ch6a2.setCompletedDate(now.minus(38, ChronoUnit.DAYS));
            ch6a2.setNotes("Peak weekend rush handled with zero errors.");

            OrderFollowupCheck ch6a3 = new OrderFollowupCheck(o6a, "30_DAYS", "Last 30 Days", 30);
            ch6a3.setCompleted(true);
            ch6a3.setStatus("COMPLETED");
            ch6a3.setCompletedDate(now.minus(15, ChronoUnit.DAYS));
            ch6a3.setNotes("30 days audit passed.");

            followupCheckRepository.save(ch6a1);
            followupCheckRepository.save(ch6a2);
            followupCheckRepository.save(ch6a3);

            CustomerOrder o6b = new CustomerOrder("ORD-107", c6, "Extra Kitchen Printers (x2) & Backup Cable Kit", 2, 780.0, now.minus(12, ChronoUnit.DAYS));
            o6b = orderRepository.save(o6b);

            OrderFollowupCheck ch6b1 = new OrderFollowupCheck(o6b, "24_HOURS", "Last 24 Hours", 1);
            ch6b1.setCompleted(true);
            ch6b1.setStatus("COMPLETED");
            ch6b1.setCompletedDate(now.minus(11, ChronoUnit.DAYS));

            OrderFollowupCheck ch6b2 = new OrderFollowupCheck(o6b, "7_DAYS", "Last 7 Days", 7);
            ch6b2.setCompleted(true);
            ch6b2.setStatus("COMPLETED");
            ch6b2.setCompletedDate(now.minus(5, ChronoUnit.DAYS));

            OrderFollowupCheck ch6b3 = new OrderFollowupCheck(o6b, "30_DAYS", "Last 30 Days", 30);
            ch6b3.setCompleted(true);
            ch6b3.setStatus("COMPLETED");
            ch6b3.setCompletedDate(now.minus(1, ChronoUnit.DAYS));

            followupCheckRepository.save(ch6b1);
            followupCheckRepository.save(ch6b2);
            followupCheckRepository.save(ch6b3);

            CustomerFeedback f2 = new CustomerFeedback(c6, o6a, 5, "Reliable hardware and fantastic after-sales team in Iraq! High quality service.");
            feedbackRepository.save(f2);

            // 7. Customer: Chavi Land Retail & Cafe (ALERT - 26h ago, 24h milestone overdue)
            Customer c7 = new Customer(null, "Chavi Land Retail & Cafe", "+964 750 666 7788", "cafe@chaviland.iq", "Sulaymaniyah");
            c7.setTotalOrders(1);
            c7.setTotalSpent(540.0);
            c7.setLastOrderDate(now.minus(26, ChronoUnit.HOURS));
            c7 = customerRepository.save(c7);

            CustomerOrder o7 = new CustomerOrder("ORD-108", c7, "Wireless Handheld Ordering Tablets", 2, 540.0, now.minus(26, ChronoUnit.HOURS));
            o7 = orderRepository.save(o7);

            OrderFollowupCheck ch7a = new OrderFollowupCheck(o7, "24_HOURS", "Last 24 Hours", 1);
            ch7a.setDurationValue(24);
            ch7a.setDurationUnit("HOURS");
            ch7a.setTargetDays(1);
            ch7a.setStatus("PENDING");
            ch7a.setCompleted(false);

            OrderFollowupCheck ch7_3d = new OrderFollowupCheck(o7, "3_DAYS", "3 Days Ago", 3);
            ch7_3d.setDurationValue(3);
            ch7_3d.setDurationUnit("DAYS");
            ch7_3d.setTargetDays(3);
            ch7_3d.setStatus("PENDING");
            ch7_3d.setCompleted(false);

            OrderFollowupCheck ch7b = new OrderFollowupCheck(o7, "7_DAYS", "Last 7 Days", 7);
            ch7b.setDurationValue(7);
            ch7b.setDurationUnit("DAYS");
            ch7b.setTargetDays(7);
            ch7b.setStatus("PENDING");
            ch7b.setCompleted(false);

            OrderFollowupCheck ch7c = new OrderFollowupCheck(o7, "30_DAYS", "Last 30 Days", 30);
            ch7c.setDurationValue(30);
            ch7c.setDurationUnit("DAYS");
            ch7c.setTargetDays(30);
            ch7c.setStatus("PENDING");
            ch7c.setCompleted(false);

            followupCheckRepository.save(ch7a);
            followupCheckRepository.save(ch7_3d);
            followupCheckRepository.save(ch7b);
            followupCheckRepository.save(ch7c);

            log.info("Seeded rich realistic multi-customer practice dataset with diverse ALERT, IDLE, and DONE statuses.");
        }

        log.info("System initialization complete.");
    }
}

