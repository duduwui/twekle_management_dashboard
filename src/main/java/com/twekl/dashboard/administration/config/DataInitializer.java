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
        seedTimeFilterPresets();
        Role roleEngineer = seedRoles();
        seedSuperAdmin(roleEngineer);
        seedCustomerPracticeData();
        log.info("System initialization complete.");
    }

    private void seedTimeFilterPresets() {
        if (timeFilterPresetRepository.count() == 0) {
            log.info("Seeding Dynamic Time Filter Presets...");
            timeFilterPresetRepository.save(new TimeFilterPreset("24_HOURS", "Last 24 Hours", "آخر 24 ساعة", "٢٤ کاتژمێری ڕابردوو", 24, "HOURS", 1));
            timeFilterPresetRepository.save(new TimeFilterPreset("3_DAYS", "3 Days Ago", "منذ 3 أيام", "٣ ڕۆژ لەمەوبەر", 3, "DAYS", 2));
            timeFilterPresetRepository.save(new TimeFilterPreset("7_DAYS", "Last 7 Days", "آخر 7 أيام", "٧ ڕۆژی ڕابردوو", 7, "DAYS", 3));
            timeFilterPresetRepository.save(new TimeFilterPreset("30_DAYS", "Last 30 Days", "آخر 30 يوم", "٣٠ ڕۆژی ڕابردوو", 30, "DAYS", 4));
            timeFilterPresetRepository.save(new TimeFilterPreset("2_MONTHS", "Last 2 Months", "آخر شهرين", "٢ مانگی ڕابردوو", 2, "MONTHS", 5));
            timeFilterPresetRepository.save(new TimeFilterPreset("60_DAYS", "60 Days Review", "مراجعة 60 يوم", "پێداچوونەوەی ٦٠ ڕۆژ", 60, "DAYS", 6));
        }
    }

    private Role seedRoles() {
        seedRoleIfMissing("Software Engineer", true, true, true, true);
        seedRoleIfMissing("Sales Representative", true, true, true, false);
        seedRoleIfMissing("Product Specialist", false, true, true, false);
        seedRoleIfMissing("Read-Only Auditor", false, true, false, false);
        seedRoleIfMissing("Customer Success Lead", true, true, true, false);
        seedRoleIfMissing("Inventory Controller", false, true, true, false);
        return roleRepository.findByName("Software Engineer").orElse(null);
    }

    private void seedRoleIfMissing(String name, boolean create, boolean read, boolean update, boolean delete) {
        if (!roleRepository.existsByName(name)) {
            roleRepository.save(new Role(name, create, read, update, delete));
        }
    }

    private void seedSuperAdmin(Role roleEngineer) {
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
    }

    private void seedCustomerPracticeData() {
        if (customerRepository.count() > 0) {
            return;
        }

        log.info("Seeding Rich Customer Lifecycle Practice Dataset with Instant timestamps...");
        Instant now = Instant.now();

        feedbackRepository.deleteAll();
        followupCheckRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();

        // 1. Customer: Zana Commercial Stores (ALERT - Overdue 24h check)
        Customer c1 = saveCustomer("Zana Commercial Stores", "+964 750 445 6789", "orders@zanastores.iq", "Sulaymaniyah", 1, 1420.0, now.minus(28, ChronoUnit.HOURS));
        CustomerOrder o1 = saveOrder("ORD-101", c1, "Thermal Receipt Printers (x2) + Barcode Scanner", 2, 1420.0, now.minus(28, ChronoUnit.HOURS));
        createPendingCheck(o1, "24_HOURS", "Last 24 Hours", 24, "HOURS", 1);
        createPendingCheck(o1, "3_DAYS", "3 Days Ago", 3, "DAYS", 3);
        createPendingCheck(o1, "7_DAYS", "Last 7 Days", 7, "DAYS", 7);
        createPendingCheck(o1, "30_DAYS", "Last 30 Days", 30, "DAYS", 30);

        // 2. Customer: Barzan Electronics (IDLE - Brand new order, due in ~21h)
        Customer c2 = saveCustomer("Barzan Electronics", "+964 750 111 2233", "orders@barzantech.iq", "Erbil", 1, 850.0, now.minus(3, ChronoUnit.HOURS));
        CustomerOrder o2 = saveOrder("ORD-102", c2, "Twekl POS Touch Terminal 15.6\" + Cash Drawer", 1, 850.0, now.minus(3, ChronoUnit.HOURS));
        createPendingCheck(o2, "24_HOURS", "Last 24 Hours", 24, "HOURS", 1);
        createPendingCheck(o2, "3_DAYS", "3 Days Ago", 3, "DAYS", 3);
        createPendingCheck(o2, "7_DAYS", "Last 7 Days", 7, "DAYS", 7);
        createPendingCheck(o2, "30_DAYS", "Last 30 Days", 30, "DAYS", 30);

        // 3. Customer: Al-Mansour Supermarket (DONE - All milestones checked & verified)
        Customer c3 = saveCustomer("Al-Mansour Supermarket", "+964 770 555 8899", "admin@almansour-market.iq", "Baghdad", 1, 3850.0, now.minus(35, ChronoUnit.DAYS));
        CustomerOrder o3 = saveOrder("ORD-103", c3, "Retail POS Full Bundle + Heavy Duty Cash Drawer", 1, 3850.0, now.minus(35, ChronoUnit.DAYS));
        createCompletedCheck(o3, "24_HOURS", "Last 24 Hours", 24, "HOURS", 1, now.minus(34, ChronoUnit.DAYS), "Sara Hassan", "5", "Initial setup and calibration verified with store manager.");
        createCompletedCheck(o3, "3_DAYS", "3 Days Ago", 3, "DAYS", 3, now.minus(32, ChronoUnit.DAYS), "Sara Hassan", "5", "Setup verified.");
        createCompletedCheck(o3, "7_DAYS", "Last 7 Days", 7, "DAYS", 7, now.minus(28, ChronoUnit.DAYS), "Ahmed Al-Iraqi", "5", "Cashiers trained successfully; receipt printing working smoothly.");
        createCompletedCheck(o3, "30_DAYS", "Last 30 Days", 30, "DAYS", 30, now.minus(5, ChronoUnit.DAYS), "Kawa Barzani", "5", "Monthly review complete. Client requested quote for 2 extra branches.");
        feedbackRepository.save(new CustomerFeedback(c3, o3, 5, "POS terminal setup was very smooth and fast. Great customer support in Baghdad!"));

        // 4. Customer: Hawler Pharmacy Chain (ALERT - 24h done, 7 Days overdue)
        Customer c4 = saveCustomer("Hawler Pharmacy Chain", "+964 750 789 0123", "contact@hawlerpharmacy.iq", "Erbil", 1, 1250.0, now.minus(9, ChronoUnit.DAYS));
        CustomerOrder o4 = saveOrder("ORD-104", c4, "Medicinal Barcode Readers & POS Hub", 1, 1250.0, now.minus(9, ChronoUnit.DAYS));
        createCompletedCheck(o4, "24_HOURS", "Last 24 Hours", 24, "HOURS", 1, now.minus(8, ChronoUnit.DAYS), "Ahmed Al-Iraqi", "4", "Delivery confirmed. Device online.");
        createCompletedCheck(o4, "3_DAYS", "3 Days Ago", 3, "DAYS", 3, now.minus(6, ChronoUnit.DAYS), "Ahmed Al-Iraqi", "4", "Initial 3-day verification done.");
        createPendingCheck(o4, "7_DAYS", "Last 7 Days", 7, "DAYS", 7);
        createPendingCheck(o4, "30_DAYS", "Last 30 Days", 30, "DAYS", 30);

        // 5. Customer: Korek Telecom Center (IDLE - 24h done, 7 Days due in 3 days)
        Customer c5 = saveCustomer("Korek Telecom Center", "+964 751 333 4455", "support@korekcenter.iq", "Duhok", 1, 670.0, now.minus(4, ChronoUnit.DAYS));
        CustomerOrder o5 = saveOrder("ORD-105", c5, "Customer Queue Screen & Mini Kiosk", 1, 670.0, now.minus(4, ChronoUnit.DAYS));
        createCompletedCheck(o5, "24_HOURS", "Last 24 Hours", 24, "HOURS", 1, now.minus(3, ChronoUnit.DAYS), "Sara Hassan", "5", "Kiosk operational in main Duhok showroom.");
        createCompletedCheck(o5, "3_DAYS", "3 Days Ago", 3, "DAYS", 3, now.minus(1, ChronoUnit.DAYS), "Sara Hassan", "5", "3-day check passed.");
        createPendingCheck(o5, "7_DAYS", "Last 7 Days", 7, "DAYS", 7);
        createPendingCheck(o5, "30_DAYS", "Last 30 Days", 30, "DAYS", 30);

        // 6. Customer: Babylon Restaurant Group (DONE - Multi-order VIP)
        Customer c6 = saveCustomer("Babylon Restaurant Group", "+964 771 888 9900", "management@babylonrest.iq", "Baghdad", 2, 2930.0, now.minus(12, ChronoUnit.DAYS));
        CustomerOrder o6a = saveOrder("ORD-106", c6, "Kitchen Display System (KDS) + 3x Heavy Printers", 1, 2150.0, now.minus(45, ChronoUnit.DAYS));
        createCompletedCheck(o6a, "24_HOURS", "Last 24 Hours", 1, "HOURS", 1, now.minus(44, ChronoUnit.DAYS), null, null, "Kitchen staff trained on KDS display.");
        createCompletedCheck(o6a, "7_DAYS", "Last 7 Days", 7, "DAYS", 7, now.minus(38, ChronoUnit.DAYS), null, null, "Peak weekend rush handled with zero errors.");
        createCompletedCheck(o6a, "30_DAYS", "Last 30 Days", 30, "DAYS", 30, now.minus(15, ChronoUnit.DAYS), null, null, "30 days audit passed.");

        CustomerOrder o6b = saveOrder("ORD-107", c6, "Extra Kitchen Printers (x2) & Backup Cable Kit", 2, 780.0, now.minus(12, ChronoUnit.DAYS));
        createCompletedCheck(o6b, "24_HOURS", "Last 24 Hours", 1, "HOURS", 1, now.minus(11, ChronoUnit.DAYS), null, null, null);
        createCompletedCheck(o6b, "7_DAYS", "Last 7 Days", 7, "DAYS", 7, now.minus(5, ChronoUnit.DAYS), null, null, null);
        createCompletedCheck(o6b, "30_DAYS", "Last 30 Days", 30, "DAYS", 30, now.minus(1, ChronoUnit.DAYS), null, null, null);
        feedbackRepository.save(new CustomerFeedback(c6, o6a, 5, "Reliable hardware and fantastic after-sales team in Iraq! High quality service."));

        // 7. Customer: Chavi Land Retail & Cafe (ALERT - 26h ago, 24h milestone overdue)
        Customer c7 = saveCustomer("Chavi Land Retail & Cafe", "+964 750 666 7788", "cafe@chaviland.iq", "Sulaymaniyah", 1, 540.0, now.minus(26, ChronoUnit.HOURS));
        CustomerOrder o7 = saveOrder("ORD-108", c7, "Wireless Handheld Ordering Tablets", 2, 540.0, now.minus(26, ChronoUnit.HOURS));
        createPendingCheck(o7, "24_HOURS", "Last 24 Hours", 24, "HOURS", 1);
        createPendingCheck(o7, "3_DAYS", "3 Days Ago", 3, "DAYS", 3);
        createPendingCheck(o7, "7_DAYS", "Last 7 Days", 7, "DAYS", 7);
        createPendingCheck(o7, "30_DAYS", "Last 30 Days", 30, "DAYS", 30);

        log.info("Seeded rich realistic multi-customer practice dataset with diverse ALERT, IDLE, and DONE statuses.");
    }

    private Customer saveCustomer(String name, String phone, String email, String city, int totalOrders, double spent, Instant lastOrder) {
        Customer c = new Customer(null, name, phone, email, city);
        c.setTotalOrders(totalOrders);
        c.setTotalSpent(spent);
        c.setLastOrderDate(lastOrder);
        return customerRepository.save(c);
    }

    private CustomerOrder saveOrder(String orderNum, Customer customer, String summary, int qty, double amount, Instant date) {
        CustomerOrder o = new CustomerOrder(orderNum, customer, summary, qty, amount, date);
        return orderRepository.save(o);
    }

    private void createPendingCheck(CustomerOrder order, String presetKey, String name, int val, String unit, int targetDays) {
        OrderFollowupCheck ch = new OrderFollowupCheck(order, presetKey, name, targetDays);
        ch.setDurationValue(val);
        ch.setDurationUnit(unit);
        ch.setTargetDays(targetDays);
        ch.setStatus("PENDING");
        ch.setCompleted(false);
        followupCheckRepository.save(ch);
    }

    private void createCompletedCheck(CustomerOrder order, String presetKey, String name, int val, String unit, int targetDays, 
                                      Instant completedDate, String checkedBy, String satisfaction, String notes) {
        OrderFollowupCheck ch = new OrderFollowupCheck(order, presetKey, name, targetDays);
        ch.setDurationValue(val);
        ch.setDurationUnit(unit);
        ch.setTargetDays(targetDays);
        ch.setCompleted(true);
        ch.setStatus("COMPLETED");
        ch.setCompletedDate(completedDate);
        ch.setCheckedBy(checkedBy);
        ch.setSatisfaction(satisfaction);
        ch.setNotes(notes);
        followupCheckRepository.save(ch);
    }
}
