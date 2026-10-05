package com.twekl.dashboard.common.config;

import com.twekl.dashboard.modules.admin.entity.Admin;
import com.twekl.dashboard.modules.admin.repository.AdminRepository;
import com.twekl.dashboard.modules.customer.entity.Customer;
import com.twekl.dashboard.modules.customer.entity.CustomerOrder;
import com.twekl.dashboard.modules.customer.entity.OrderFollowupCheck;
import com.twekl.dashboard.modules.customer.repository.CustomerFeedbackRepository;
import com.twekl.dashboard.modules.customer.repository.CustomerOrderRepository;
import com.twekl.dashboard.modules.customer.repository.CustomerRepository;
import com.twekl.dashboard.modules.customer.repository.OrderFollowupCheckRepository;
import com.twekl.dashboard.modules.role.entity.Role;
import com.twekl.dashboard.modules.role.repository.RoleRepository;
import com.twekl.dashboard.modules.timefilter.entity.TimeFilterPreset;
import com.twekl.dashboard.modules.timefilter.repository.TimeFilterPresetRepository;
import com.twekl.dashboard.modules.user.entity.AppUser;
import com.twekl.dashboard.modules.user.entity.ModulePermission;
import com.twekl.dashboard.modules.user.repository.AppUserRepository;
import com.twekl.dashboard.modules.user.repository.ModulePermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DataInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final AppUserRepository appUserRepository;
    private final ModulePermissionRepository permissionRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;
    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(AdminRepository adminRepository, 
                           RoleRepository roleRepository,
                           AppUserRepository appUserRepository,
                           ModulePermissionRepository permissionRepository,
                           TimeFilterPresetRepository timeFilterPresetRepository,
                           CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           OrderFollowupCheckRepository followupCheckRepository,
                           PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.appUserRepository = appUserRepository;
        this.permissionRepository = permissionRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.followupCheckRepository = followupCheckRepository;
        this.passwordEncoder = passwordEncoder;
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
            timeFilterPresetRepository.save(new TimeFilterPreset("7_DAYS", "Last 7 Days", "آخر 7 أيام", "٧ ڕۆژی ڕابردوو", 7, "DAYS", 2));
            timeFilterPresetRepository.save(new TimeFilterPreset("30_DAYS", "Last 30 Days", "آخر 30 يوم", "٣٠ ڕۆژی ڕابردوو", 30, "DAYS", 3));
            timeFilterPresetRepository.save(new TimeFilterPreset("2_MONTHS", "Last 2 Months", "آخر شهرين", "٢ مانگی ڕابردوو", 2, "MONTHS", 4));
            timeFilterPresetRepository.save(new TimeFilterPreset("3_DAYS", "3 Days Ago", "منذ 3 أيام", "٣ ڕۆژ لەمەوبەر", 3, "DAYS", 5));
            timeFilterPresetRepository.save(new TimeFilterPreset("60_DAYS", "60 Days Review", "مراجعة 60 يوم", "پێداچوونەوەی ٦٠ ڕۆژ", 60, "DAYS", 6));
        }

        if (roleRepository.count() == 0) {
            log.info("Seeding Default Role Templates...");
            roleRepository.save(new Role("Software Engineer", true, true, true, true));
            roleRepository.save(new Role("Sales Representative", true, true, true, false));
            roleRepository.save(new Role("Product Specialist", false, true, true, false));
            roleRepository.save(new Role("Read-Only Auditor", false, true, false, false));
        }

        if (!adminRepository.existsByUsername("twekl_super_admin")) {
            log.info("Seeding Super Admin twekl_super_admin...");
            Admin superAdmin = Admin.builder()
                    .username("twekl_super_admin")
                    .password(passwordEncoder.encode("Super@2026"))
                    .phoneNumber("+966 50 111 2233")
                    .isSuperAdmin(true)
                    .canCreateRoles(true)
                    .canManageUsers(true)
                    .status("ACTIVE")
                    .build();
            adminRepository.save(superAdmin);
        }

        if (appUserRepository.count() == 0) {
            log.info("Seeding Sample App Users with 3-Module CRUD Permissions...");

            // User 1: Ahmed Al-Iraqi (Full Software & Sales access)
            AppUser u1 = new AppUser(null, "ahmed_tech", "أحمد العراقي", "ئەحمەد عێراقی", passwordEncoder.encode("User@2026"), "+964 750 123 4567", "ACTIVE");
            u1.addPermission(new ModulePermission("software", "Software Management", "إدارة البرمجيات", "بەڕێوەبردنی سۆفتوێر", true, true, true, true));
            u1.addPermission(new ModulePermission("sales", "Sales & Invoices", "المبيعات والفواتير", "فرۆشتن و پسوولەکان", true, true, true, false));
            u1.addPermission(new ModulePermission("products", "Product Management", "إدارة المنتجات", "بەڕێوەبردنی کاڵاکان", true, true, true, false));
            appUserRepository.save(u1);

            // User 2: Sara Hassan (Sales Representative)
            AppUser u2 = new AppUser(null, "sara_sales", "سارة حسان", "سارا حەسەن", passwordEncoder.encode("User@2026"), "+964 770 987 6543", "ACTIVE");
            u2.addPermission(new ModulePermission("software", "Software Management", "إدارة البرمجيات", "بەڕێوەبردنی سۆفتوێر", false, true, false, false));
            u2.addPermission(new ModulePermission("sales", "Sales & Invoices", "المبيعات والفواتير", "فرۆشتن و پسوولەکان", true, true, true, true));
            u2.addPermission(new ModulePermission("products", "Product Management", "إدارة المنتجات", "بەڕێوەبردنی کاڵاکان", false, true, true, false));
            appUserRepository.save(u2);

            // User 3: Kawa Barzani (Product Specialist)
            AppUser u3 = new AppUser(null, "kawa_products", "كاوا بارزاني", "کاوە بارزانی", passwordEncoder.encode("User@2026"), "+964 751 222 3344", "ACTIVE");
            u3.addPermission(new ModulePermission("software", "Software Management", "إدارة البرمجيات", "بەڕێوەبردنی سۆفتوێر", false, true, false, false));
            u3.addPermission(new ModulePermission("sales", "Sales & Invoices", "المبيعات والفواتير", "فرۆشتن و پسوولەکان", false, true, false, false));
            u3.addPermission(new ModulePermission("products", "Product Management", "إدارة المنتجات", "بەڕێوەبردنی کاڵاکان", true, true, true, true));
            appUserRepository.save(u3);

            log.info("Seeded 3 sample multilingual App Users with permissions.");
        }

        if (customerRepository.count() == 0) {
            log.info("Seeding Customer Lifecycle Dataset...");
            LocalDateTime now = LocalDateTime.now();

            Customer c1 = new Customer(null, "Barzan Electronics", "+964 750 111 2233", "orders@barzantech.iq", "Erbil");
            c1.setTotalOrders(1);
            c1.setTotalSpent(850.0);
            c1.setLastOrderDate(now.minusMinutes(34));
            c1 = customerRepository.save(c1);

            CustomerOrder o1 = new CustomerOrder("ORD-001", c1, "Twekl POS Touch Terminal 15.6\" + Cash Drawer", 1, 850.0, now.minusMinutes(34));
            o1 = orderRepository.save(o1);

            OrderFollowupCheck ch1 = new OrderFollowupCheck(o1, "24_HOURS", "24 Hours Ago", 1);
            OrderFollowupCheck ch2 = new OrderFollowupCheck(o1, "7_DAYS", "1 Week Ago", 7);
            OrderFollowupCheck ch3 = new OrderFollowupCheck(o1, "30_DAYS", "1 Month Ago", 30);
            followupCheckRepository.save(ch1);
            followupCheckRepository.save(ch2);
            followupCheckRepository.save(ch3);

            Customer c2 = new Customer(null, "Zana Commercial Stores", "+964 750 445 6789", "orders@zanastores.iq", "Sulaymaniyah");
            c2.setTotalOrders(1);
            c2.setTotalSpent(1420.0);
            c2.setLastOrderDate(now.minusHours(4).minusMinutes(22));
            c2 = customerRepository.save(c2);

            CustomerOrder o2 = new CustomerOrder("ORD-002", c2, "Thermal Receipt Printers (x2) + Barcode Scanner", 2, 1420.0, now.minusHours(4).minusMinutes(22));
            o2 = orderRepository.save(o2);

            followupCheckRepository.save(new OrderFollowupCheck(o2, "24_HOURS", "24 Hours Ago", 1));
            followupCheckRepository.save(new OrderFollowupCheck(o2, "7_DAYS", "1 Week Ago", 7));
            followupCheckRepository.save(new OrderFollowupCheck(o2, "30_DAYS", "1 Month Ago", 30));

            Customer c3 = new Customer(null, "Al-Mansour Supermarket", "+964 770 555 8899", "admin@almansour-market.iq", "Baghdad");
            c3.setTotalOrders(1);
            c3.setTotalSpent(3850.0);
            c3.setLastOrderDate(now.minusHours(26).minusMinutes(15));
            c3 = customerRepository.save(c3);

            CustomerOrder o3 = new CustomerOrder("ORD-003", c3, "Retail POS Full Bundle + Heavy Duty Cash Drawer", 1, 3850.0, now.minusHours(26).minusMinutes(15));
            o3 = orderRepository.save(o3);

            OrderFollowupCheck ch3a = new OrderFollowupCheck(o3, "24_HOURS", "24 Hours Ago", 1);
            ch3a.setNotes("Milestone reached (26h old). Awaiting staff call.");
            followupCheckRepository.save(ch3a);
            followupCheckRepository.save(new OrderFollowupCheck(o3, "7_DAYS", "1 Week Ago", 7));
            followupCheckRepository.save(new OrderFollowupCheck(o3, "30_DAYS", "1 Month Ago", 30));

            log.info("Seeded initial customers, orders, and followup checkpoints.");
        }

        log.info("System initialization complete.");
    }
}
