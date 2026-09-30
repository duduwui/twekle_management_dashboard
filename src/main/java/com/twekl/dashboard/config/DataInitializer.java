package com.twekl.dashboard.config;

import com.twekl.dashboard.model.*;
import com.twekl.dashboard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminRepository adminRepository;
    private final AppUserRepository userRepository;
    private final ModulePermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository orderRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final TimeFilterPresetRepository timeFilterPresetRepository;
    private final OrderFollowupCheckRepository followupCheckRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    public DataInitializer(AdminRepository adminRepository, 
                           AppUserRepository userRepository, 
                           ModulePermissionRepository permissionRepository,
                           RoleRepository roleRepository,
                           CustomerRepository customerRepository,
                           CustomerOrderRepository orderRepository,
                           CustomerFeedbackRepository feedbackRepository,
                           TimeFilterPresetRepository timeFilterPresetRepository,
                           OrderFollowupCheckRepository followupCheckRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
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
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("24 Hours Ago").durationValue(24).durationUnit("HOURS").isActive(true).build());
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("7 Days Ago").durationValue(7).durationUnit("DAYS").isActive(true).build());
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("2 Weeks Ago").durationValue(14).durationUnit("DAYS").isActive(true).build());
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("30+ Days Dormant").durationValue(30).durationUnit("DAYS").isActive(true).build());
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

        if (!adminRepository.existsByUsername("ops_admin")) {
            log.info("Seeding Operations Admin ops_admin...");
            adminRepository.save(Admin.builder()
                    .username("ops_admin")
                    .password(passwordEncoder.encode("Admin@2026"))
                    .phoneNumber("+966 55 444 5566")
                    .isSuperAdmin(false)
                    .canCreateRoles(true)
                    .canManageUsers(true)
                    .status("ACTIVE")
                    .build());
        }

        if (userRepository.count() == 0) {
            log.info("Seeding Users with 3 Languages & 3-Column Module CRUD Cards...");

            // User 1: Software Engineer
            AppUser user1 = userRepository.save(AppUser.builder()
                    .usernameEn("tariq_dev")
                    .usernameAr("طارق منصور")
                    .usernameKu("تاریق مەنسوور")
                    .password(passwordEncoder.encode("DevPass#123"))
                    .phoneNumber("+964 750 123 4567")
                    .status("ACTIVE")
                    .build());

            List<ModulePermission> user1Perms = new ArrayList<>();
            user1Perms.add(ModulePermission.builder()
                    .userId(user1.getId())
                    .moduleKey("SOFTWARE")
                    .moduleNameEn("Software & Development")
                    .moduleNameAr("تطوير البرمجيات والأنظمة")
                    .moduleNameKu("پەرەپێدانی سۆفتوێر")
                    .descriptionEn("Source code repositories, deployments, API services, and dev tools")
                    .descriptionAr("مستودعات الكود، خدمات الواجهات البرمجية، وأدوات المطورين")
                    .descriptionKu("کۆگاکانی کۆد، خزمەتگوزاریەکانی API، و ئامرازەکانی پەرەپێدەران")
                    .canCreate(true).canRead(true).canUpdate(true).canDelete(true).isVisible(true)
                    .badgeColor("#4F46E5")
                    .build());

            user1Perms.add(ModulePermission.builder()
                    .userId(user1.getId())
                    .moduleKey("SALES")
                    .moduleNameEn("Sales & Revenue")
                    .moduleNameAr("المبيعات والإيرادات")
                    .moduleNameKu("فرۆشتن و داهات")
                    .descriptionEn("Invoices, transaction pipeline, client contracts, and financial reports")
                    .descriptionAr("الفواتير، خط المبيعات، عقود العملاء، والتقارير المالية")
                    .descriptionKu("پسوولەکان، گرێبەستی کڕیاران، و ڕاپۆرتە داراییەکان")
                    .canCreate(false).canRead(true).canUpdate(false).canDelete(false).isVisible(true)
                    .badgeColor("#35B89F")
                    .build());

            user1Perms.add(ModulePermission.builder()
                    .userId(user1.getId())
                    .moduleKey("PRODUCT")
                    .moduleNameEn("Product Management")
                    .moduleNameAr("إدارة المنتجات")
                    .moduleNameKu("بەڕێوەبردنی بەرهەمەکان")
                    .descriptionEn("Product catalogs, inventory levels, release roadmaps, and feature logs")
                    .descriptionAr("كتالوج المنتجات، مستويات المخزون، وخطط إطلاق الميزات")
                    .descriptionKu("کاتالۆگی بەرهەمەکان، ئاستی مەخزەن، و نەخشەی کار")
                    .canCreate(false).canRead(true).canUpdate(true).canDelete(false).isVisible(true)
                    .badgeColor("#0EA5E9")
                    .build());
            permissionRepository.saveAll(user1Perms);
        }

        // Seed Customers & Follow-up data
        if (customerRepository.count() == 0) {
            log.info("Seeding Customer Follow-up Profiles, Order Logs, and Feedback/Compliments...");
            LocalDateTime now = LocalDateTime.now();

            // Customer 1: Dara Ahmed (3 orders)
            Customer c1 = customerRepository.save(Customer.builder()
                    .name("Dara Ahmed")
                    .phoneNumber("+964 750 445 6789")
                    .email("dara.ahmed@erbilmarket.com")
                    .city("Erbil")
                    .totalOrders(3)
                    .totalSpent(850.0)
                    .lastOrderDate(now.minusHours(24))
                    .daysSinceLastOrder(1)
                    .status("FOLLOW_UP_24H")
                    .build());

            orderRepository.save(CustomerOrder.builder().customerId(c1.getId()).orderNumber("ORD-801").orderDate(now.minusHours(24)).itemsSummary("Twekl POS Cloud Subscription + Wireless Barcode Scanner").totalAmount(450.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c1.getId()).orderNumber("ORD-720").orderDate(now.minusDays(18)).itemsSummary("Thermal Receipt Paper Box (x50 rolls)").totalAmount(120.0).paymentMethod("Cash on Delivery").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c1.getId()).orderNumber("ORD-650").orderDate(now.minusDays(45)).itemsSummary("Touchscreen Display Stand Bracket").totalAmount(280.0).paymentMethod("Credit Card").orderStatus("DELIVERED").build());

            feedbackRepository.save(CustomerFeedback.builder().customerId(c1.getId()).customerName(c1.getName()).authorName("Agent Tariq").feedbackType("COMPLIMENT").content("Customer called expressing great appreciation for the quick 2-hour onboarding process! Requested extra thermal paper in next cycle.").rating(5).status("FOLLOWED_UP").createdAt(now.minusHours(20)).build());

            // Customer 2: Layla Al-Khatib (1 order)
            Customer c2 = customerRepository.save(Customer.builder()
                    .name("Layla Al-Khatib")
                    .phoneNumber("+964 770 123 9988")
                    .email("layla.khatib@baghdadretail.iq")
                    .city("Baghdad")
                    .totalOrders(1)
                    .totalSpent(340.0)
                    .lastOrderDate(now.minusHours(23))
                    .daysSinceLastOrder(1)
                    .status("FOLLOW_UP_24H")
                    .build());

            orderRepository.save(CustomerOrder.builder().customerId(c2.getId()).orderNumber("ORD-805").orderDate(now.minusHours(23)).itemsSummary("Inventory Handheld Scanner Bundle (2 Units)").totalAmount(340.0).paymentMethod("ZainCash").orderStatus("DELIVERED").build());
            feedbackRepository.save(CustomerFeedback.builder().customerId(c2.getId()).customerName(c2.getName()).authorName("Ops Super Admin").feedbackType("COMPLIMENT").content("First-time customer compliment: 'The courier delivery to Mansour was lightning fast and items were well packaged.'").rating(5).status("FOLLOWED_UP").createdAt(now.minusHours(18)).build());

            // Customer 3: Soran Hawrami (5 orders)
            Customer c3 = customerRepository.save(Customer.builder()
                    .name("Soran Hawrami")
                    .phoneNumber("+964 750 888 1234")
                    .email("soran.hawrami@suli-cafe.com")
                    .city("Sulaymaniyah")
                    .totalOrders(5)
                    .totalSpent(2150.0)
                    .lastOrderDate(now.minusDays(7))
                    .daysSinceLastOrder(7)
                    .status("FOLLOW_UP_7D")
                    .build());

            orderRepository.save(CustomerOrder.builder().customerId(c3.getId()).orderNumber("ORD-780").orderDate(now.minusDays(7)).itemsSummary("POS Dual-Screen Android Terminal (Master Station)").totalAmount(890.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c3.getId()).orderNumber("ORD-710").orderDate(now.minusDays(22)).itemsSummary("Thermal Kitchen Ticket Printer (LAN / WiFi)").totalAmount(320.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c3.getId()).orderNumber("ORD-688").orderDate(now.minusDays(35)).itemsSummary("Heavy Duty Cash Drawer + Barcode Stand").totalAmount(450.0).paymentMethod("Credit Card").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c3.getId()).orderNumber("ORD-640").orderDate(now.minusDays(50)).itemsSummary("5-Port Gigabit Network Switch & Cable Bundle").totalAmount(280.0).paymentMethod("Cash on Delivery").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c3.getId()).orderNumber("ORD-590").orderDate(now.minusDays(70)).itemsSummary("Thermal Paper Super Saver Pack (80mm x 80mm)").totalAmount(210.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());

            feedbackRepository.save(CustomerFeedback.builder().customerId(c3.getId()).customerName(c3.getName()).authorName("Agent Sara").feedbackType("NOTE").content("7-Day follow-up checkup completed: Terminal is functioning smoothly in their Saholaka branch. Scheduled quarterly hardware maintenance.").rating(5).status("FOLLOWED_UP").createdAt(now.minusDays(6)).build());

            // Customer 4: Zana Rashid (2 orders)
            Customer c4 = customerRepository.save(Customer.builder()
                    .name("Zana Rashid")
                    .phoneNumber("+964 750 333 5522")
                    .email("zana.rashid@duhokmarket.com")
                    .city("Duhok")
                    .totalOrders(2)
                    .totalSpent(780.0)
                    .lastOrderDate(now.minusDays(14))
                    .daysSinceLastOrder(14)
                    .status("FOLLOW_UP_2W")
                    .build());

            orderRepository.save(CustomerOrder.builder().customerId(c4.getId()).orderNumber("ORD-775").orderDate(now.minusDays(7)).itemsSummary("Twekl Retail POS Annual License Renewal").totalAmount(520.0).paymentMethod("Credit Card").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c4.getId()).orderNumber("ORD-702").orderDate(now.minusDays(25)).itemsSummary("Handheld Wireless Inventory Scanner").totalAmount(260.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());

            feedbackRepository.save(CustomerFeedback.builder().customerId(c4.getId()).customerName(c4.getName()).authorName("Agent Tariq").feedbackType("REVIEW").content("Customer feedback: 'Software is very fast compared to old desktop systems. Staff learned it in 15 minutes.'").rating(5).status("RESOLVED").createdAt(now.minusDays(5)).build());

            // Customer 5: Rebwar Barzan (8 orders)
            Customer c5 = customerRepository.save(Customer.builder()
                    .name("Rebwar Barzan")
                    .phoneNumber("+964 750 666 4411")
                    .email("rebwar.barzan@erbil-plaza.com")
                    .city("Erbil")
                    .totalOrders(8)
                    .totalSpent(4200.0)
                    .lastOrderDate(now.minusDays(32))
                    .daysSinceLastOrder(32)
                    .status("DORMANT_30D")
                    .build());

            orderRepository.save(CustomerOrder.builder().customerId(c5.getId()).orderNumber("ORD-690").orderDate(now.minusDays(32)).itemsSummary("Full Restaurant POS Multi-Terminal Hardware Set (3 Stations)").totalAmount(2200.0).paymentMethod("Bank Transfer").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c5.getId()).orderNumber("ORD-662").orderDate(now.minusDays(40)).itemsSummary("Kitchen Display System (KDS) Touch Monitor 15-inch").totalAmount(480.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c5.getId()).orderNumber("ORD-635").orderDate(now.minusDays(52)).itemsSummary("Thermal Receipt Printers LAN/WiFi (x2 Units)").totalAmount(350.0).paymentMethod("Cash on Delivery").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c5.getId()).orderNumber("ORD-601").orderDate(now.minusDays(65)).itemsSummary("Heavy Duty Metal Cash Drawers (x2 Units)").totalAmount(280.0).paymentMethod("Credit Card").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c5.getId()).orderNumber("ORD-570").orderDate(now.minusDays(78)).itemsSummary("Digital Weighing Scale RS232 Interface").totalAmount(220.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c5.getId()).orderNumber("ORD-545").orderDate(now.minusDays(90)).itemsSummary("Wireless Handheld Ordering Tablets (x2 Units)").totalAmount(290.0).paymentMethod("Bank Transfer").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c5.getId()).orderNumber("ORD-512").orderDate(now.minusDays(105)).itemsSummary("Barcode Label Printer & Thermal Rolls Pack").totalAmount(210.0).paymentMethod("Cash on Delivery").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c5.getId()).orderNumber("ORD-480").orderDate(now.minusDays(120)).itemsSummary("Magnetic Stripe & RFID Card Reader Terminals").totalAmount(170.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());

            feedbackRepository.save(CustomerFeedback.builder().customerId(c5.getId()).customerName(c5.getName()).authorName("Ops Super Admin").feedbackType("NOTE").content("30+ Day Re-engagement alert: Client has been dormant for 32 days. Propose sending special 15% discount for their second branch expansion.").rating(4).status("PENDING").createdAt(now.minusDays(2)).build());

            // Customer 6: Sara Mohammed (4 orders)
            Customer c6 = customerRepository.save(Customer.builder()
                    .name("Sara Mohammed")
                    .phoneNumber("+964 771 999 3322")
                    .email("sara.mohammed@basrapharma.com")
                    .city("Basra")
                    .totalOrders(4)
                    .totalSpent(1890.0)
                    .lastOrderDate(now.minusDays(45))
                    .daysSinceLastOrder(45)
                    .status("DORMANT_30D")
                    .build());

            orderRepository.save(CustomerOrder.builder().customerId(c6.getId()).orderNumber("ORD-620").orderDate(now.minusDays(45)).itemsSummary("Pharmacy Barcode Matrix Scanners (x4)").totalAmount(760.0).paymentMethod("ZainCash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c6.getId()).orderNumber("ORD-580").orderDate(now.minusDays(60)).itemsSummary("Pharmacy Label Printer & Barcode Verification Reader").totalAmount(450.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c6.getId()).orderNumber("ORD-530").orderDate(now.minusDays(75)).itemsSummary("Secure Cash Drawer & Countertop Display").totalAmount(380.0).paymentMethod("Cash on Delivery").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c6.getId()).orderNumber("ORD-495").orderDate(now.minusDays(95)).itemsSummary("Uninterruptible Power Supply (UPS 1500VA)").totalAmount(300.0).paymentMethod("Credit Card").orderStatus("DELIVERED").build());

            feedbackRepository.save(CustomerFeedback.builder().customerId(c6.getId()).customerName(c6.getName()).authorName("Agent Tariq").feedbackType("INQUIRY").content("Customer inquired about automated expiry date tracking module. Follow-up scheduled for next week.").rating(4).status("PENDING").createdAt(now.minusDays(10)).build());

            // Customer 7: John Smith (6 orders)
            Customer c7 = customerRepository.save(Customer.builder()
                    .name("John Smith")
                    .phoneNumber("+964 750 999 8877")
                    .email("john_smith@twekl-partner.com")
                    .city("Erbil")
                    .totalOrders(6)
                    .totalSpent(2850.0)
                    .lastOrderDate(now.minusHours(4))
                    .daysSinceLastOrder(0)
                    .status("FOLLOW_UP_24H")
                    .build());

            orderRepository.save(CustomerOrder.builder().customerId(c7.getId()).orderNumber("ORD-990").orderDate(now.minusHours(4)).itemsSummary("Cloud POS Enterprise Master Station").totalAmount(1150.0).paymentMethod("Credit Card").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c7.getId()).orderNumber("ORD-965").orderDate(now.minusDays(5)).itemsSummary("High-Speed 2D Barcode Imagers (x2 Units)").totalAmount(420.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c7.getId()).orderNumber("ORD-930").orderDate(now.minusDays(12)).itemsSummary("WiFi Kitchen Impact Slip Printer").totalAmount(380.0).paymentMethod("Credit Card").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c7.getId()).orderNumber("ORD-890").orderDate(now.minusDays(20)).itemsSummary("Customer Facing LED Pole Display").totalAmount(340.0).paymentMethod("Cash on Delivery").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c7.getId()).orderNumber("ORD-845").orderDate(now.minusDays(35)).itemsSummary("Thermal Receipt Paper Super Saver Pack (100 rolls)").totalAmount(310.0).paymentMethod("FIB Cash").orderStatus("DELIVERED").build());
            orderRepository.save(CustomerOrder.builder().customerId(c7.getId()).orderNumber("ORD-810").orderDate(now.minusDays(55)).itemsSummary("Biometric Staff Clock-in Reader").totalAmount(250.0).paymentMethod("Bank Transfer").orderStatus("DELIVERED").build());

            feedbackRepository.save(CustomerFeedback.builder().customerId(c7.getId()).customerName(c7.getName()).authorName("Agent John").feedbackType("COMPLIMENT").content("Client thrilled with system onboarding and fast responses.").rating(5).status("FOLLOWED_UP").createdAt(now.minusHours(2)).build());
        }

        // Auto-synchronize and backfill any missing orders for existing DB records so order count matches totalOrders exactly
        LocalDateTime now = LocalDateTime.now();
        List<Customer> existingCustomers = customerRepository.findAll();
        for (Customer cust : existingCustomers) {
            List<CustomerOrder> custOrders = orderRepository.findByCustomerIdOrderByOrderDateDesc(cust.getId());
            int currentCount = custOrders.size();
            int targetCount = (cust.getTotalOrders() != null && cust.getTotalOrders() > 0) ? cust.getTotalOrders() : Math.max(currentCount, 1);

            if (currentCount < targetCount) {
                log.info("Backfilling {} missing orders for customer: {}", (targetCount - currentCount), cust.getName());
                for (int k = currentCount + 1; k <= targetCount; k++) {
                    int orderNum = 600 + (int)(Math.random() * 390);
                    double amount = 150.0 + (int)(Math.random() * 400);
                    String[] sampleItems = {
                        "Thermal Receipt Paper Box (x50 rolls)",
                        "Wireless Barcode Scanner & Charging Cradle",
                        "Touchscreen Display Bracket Stand",
                        "Cash Drawer Heavy Duty 5-Bill 8-Coin",
                        "Network Thermal Kitchen Printer (LAN/WiFi)",
                        "Cloud POS Multi-device Terminal License"
                    };
                    String item = sampleItems[(k - 1) % sampleItems.length];
                    orderRepository.save(CustomerOrder.builder()
                            .customerId(cust.getId())
                            .orderNumber("ORD-" + orderNum)
                            .orderDate(now.minusDays(k * 8L + 3))
                            .itemsSummary(item)
                            .totalAmount(amount)
                            .paymentMethod(k % 2 == 0 ? "FIB Cash" : "Cash on Delivery")
                            .orderStatus("DELIVERED")
                            .build());
                }
            }

            // Sync updated totalSpent & totalOrders
            List<CustomerOrder> finalOrders = orderRepository.findByCustomerIdOrderByOrderDateDesc(cust.getId());
            cust.setTotalOrders(finalOrders.size());
            double totalSpent = finalOrders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
            cust.setTotalSpent(totalSpent);
            customerRepository.save(cust);
        }

        // Initialize follow-up checks for any orders that don't have them yet
        List<CustomerOrder> allOrders = orderRepository.findAll();
        List<TimeFilterPreset> presets = timeFilterPresetRepository.findAllByOrderByIdAsc();
        for (CustomerOrder o : allOrders) {
            List<OrderFollowupCheck> existingChecks = followupCheckRepository.findByOrderIdOrderByIdAsc(o.getId());
            if (existingChecks.isEmpty()) {
                for (TimeFilterPreset p : presets) {
                    boolean is24h = p.getName().toLowerCase().contains("24") || (p.getDurationValue() == 24 && "HOURS".equalsIgnoreCase(p.getDurationUnit()));
                    boolean is7d = p.getName().toLowerCase().contains("7") || (p.getDurationValue() == 7 && "DAYS".equalsIgnoreCase(p.getDurationUnit()));
                    boolean completed = is24h || (is7d && o.getId() % 2 == 0);
                    String note = completed ? "Follow-up completed regarding " + o.getItemsSummary() + ". Operational." : "Scheduled milestone check pending.";
                    String img = completed ? "https://images.unsplash.com/photo-1556742049-0a67c5574f73?w=300&q=80" : "";

                    followupCheckRepository.save(OrderFollowupCheck.builder()
                            .orderId(o.getId())
                            .presetId(p.getId())
                            .presetName(p.getName())
                            .durationValue(p.getDurationValue())
                            .durationUnit(p.getDurationUnit())
                            .isCompleted(completed)
                            .note(note)
                            .imageUrl(img)
                            .checkedBy(completed ? "Agent Tariq" : "")
                            .checkedAt(completed ? now.minusHours(6) : null)
                            .build());
                }
            }
        }

        // Ensure all customer feedbacks have customerName and order linkage populated
        List<CustomerFeedback> allFeedbacks = feedbackRepository.findAll();
        for (CustomerFeedback fb : allFeedbacks) {
            boolean changed = false;
            if (fb.getCustomerName() == null || fb.getCustomerName().isEmpty()) {
                customerRepository.findById(fb.getCustomerId()).ifPresent(c -> fb.setCustomerName(c.getName()));
                changed = true;
            }
            if (fb.getOrderNumber() == null || fb.getOrderNumber().isEmpty()) {
                List<CustomerOrder> orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(fb.getCustomerId());
                if (!orders.isEmpty()) {
                    CustomerOrder o = orders.get(0);
                    fb.setOrderId(o.getId());
                    fb.setOrderNumber(o.getOrderNumber());
                    fb.setOrderSummary(o.getItemsSummary());
                    changed = true;
                }
            }
            if (changed) {
                feedbackRepository.save(fb);
            }
        }

        // Seed 2 Months of rich, realistic operational data across all tabs
        seedTwoMonthRichData();
    }

    private void seedTwoMonthRichData() {
        log.info("Starting enrichment of 2-month operational dataset...");
        LocalDateTime now = LocalDateTime.now();

        // 1. Ensure Extra Time Presets
        if (timeFilterPresetRepository.findAll().stream().noneMatch(p -> p.getName().toLowerCase().contains("3 days"))) {
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("3 Days Ago").durationValue(3).durationUnit("DAYS").isActive(true).build());
        }
        if (timeFilterPresetRepository.findAll().stream().noneMatch(p -> p.getName().toLowerCase().contains("60 days"))) {
            timeFilterPresetRepository.save(TimeFilterPreset.builder().name("60 Days Review").durationValue(60).durationUnit("DAYS").isActive(true).build());
        }

        // 2. Ensure Extra Role Templates
        if (roleRepository.findAll().stream().noneMatch(r -> "Customer Success Lead".equalsIgnoreCase(r.getName()))) {
            roleRepository.save(Role.builder().name("Customer Success Lead").canCreate(true).canRead(true).canUpdate(true).canDelete(false).build());
        }
        if (roleRepository.findAll().stream().noneMatch(r -> "Inventory Controller".equalsIgnoreCase(r.getName()))) {
            roleRepository.save(Role.builder().name("Inventory Controller").canCreate(false).canRead(true).canUpdate(true).canDelete(false).build());
        }

        // 3. Ensure Extra Multilingual Users
        if (userRepository.count() < 4) {
            AppUser u2 = userRepository.save(AppUser.builder()
                    .usernameEn("sarah_sales")
                    .usernameAr("سارة كريم")
                    .usernameKu("سارە کەریم")
                    .password(passwordEncoder.encode("Sales@2026"))
                    .phoneNumber("+964 750 234 5678")
                    .status("ACTIVE")
                    .build());
            List<ModulePermission> u2p = new ArrayList<>();
            u2p.add(ModulePermission.builder().userId(u2.getId()).moduleKey("SOFTWARE").moduleNameEn("Software & Development").canCreate(false).canRead(true).canUpdate(false).canDelete(false).isVisible(true).build());
            u2p.add(ModulePermission.builder().userId(u2.getId()).moduleKey("SALES").moduleNameEn("Sales & Revenue").canCreate(true).canRead(true).canUpdate(true).canDelete(false).isVisible(true).build());
            u2p.add(ModulePermission.builder().userId(u2.getId()).moduleKey("PRODUCT").moduleNameEn("Product Management").canCreate(false).canRead(true).canUpdate(false).canDelete(false).isVisible(true).build());
            permissionRepository.saveAll(u2p);

            AppUser u3 = userRepository.save(AppUser.builder()
                    .usernameEn("rezan_pm")
                    .usernameAr("ريزان البرزاني")
                    .usernameKu("ڕێزان بارزانی")
                    .password(passwordEncoder.encode("Prod@2026"))
                    .phoneNumber("+964 750 345 6789")
                    .status("ACTIVE")
                    .build());
            List<ModulePermission> u3p = new ArrayList<>();
            u3p.add(ModulePermission.builder().userId(u3.getId()).moduleKey("SOFTWARE").moduleNameEn("Software & Development").canCreate(false).canRead(true).canUpdate(false).canDelete(false).isVisible(true).build());
            u3p.add(ModulePermission.builder().userId(u3.getId()).moduleKey("SALES").moduleNameEn("Sales & Revenue").canCreate(false).canRead(true).canUpdate(false).canDelete(false).isVisible(true).build());
            u3p.add(ModulePermission.builder().userId(u3.getId()).moduleKey("PRODUCT").moduleNameEn("Product Management").canCreate(true).canRead(true).canUpdate(true).canDelete(true).isVisible(true).build());
            permissionRepository.saveAll(u3p);

            AppUser u4 = userRepository.save(AppUser.builder()
                    .usernameEn("ahmed_ops")
                    .usernameAr("أحمد جلال")
                    .usernameKu("ئەحمەد جەلال")
                    .password(passwordEncoder.encode("Ops@2026"))
                    .phoneNumber("+964 770 456 7890")
                    .status("ACTIVE")
                    .build());
            List<ModulePermission> u4p = new ArrayList<>();
            u4p.add(ModulePermission.builder().userId(u4.getId()).moduleKey("SOFTWARE").moduleNameEn("Software & Development").canCreate(false).canRead(true).canUpdate(true).canDelete(false).isVisible(true).build());
            u4p.add(ModulePermission.builder().userId(u4.getId()).moduleKey("SALES").moduleNameEn("Sales & Revenue").canCreate(true).canRead(true).canUpdate(true).canDelete(false).isVisible(true).build());
            u4p.add(ModulePermission.builder().userId(u4.getId()).moduleKey("PRODUCT").moduleNameEn("Product Management").canCreate(false).canRead(true).canUpdate(false).canDelete(false).isVisible(true).build());
            permissionRepository.saveAll(u4p);
        }

        // 4. Seed New Customers & Distributed 60-Day Orders if needed
        if (customerRepository.count() < 16) {
            log.info("Seeding 10 new commercial customers across Kurdistan and Iraq...");

            String[] sampleProofImages = {
                "https://images.unsplash.com/photo-1556742049-0a67c5574f73?w=500&q=80",
                "https://images.unsplash.com/photo-1556742044-3c52d6e88c62?w=500&q=80",
                "https://images.unsplash.com/photo-1556740738-b6a63e27c4df?w=500&q=80",
                "https://images.unsplash.com/photo-1556741533-6e6a62bd8b49?w=500&q=80",
                "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?w=500&q=80",
                "https://images.unsplash.com/photo-1580910051074-3eb694886505?w=500&q=80"
            };

            Object[][] customerDefs = {
                {"Sulaymaniyah Bakery Co.", "Halgurd Rostam", "+964 770 334 1122", "halgurd.bakery@gmail.com", "Sulaymaniyah"},
                {"Erbil Grand Hypermarket", "Diyar Baban", "+964 750 221 8899", "diyar.baban@erbilgrand.com", "Erbil"},
                {"Duhok Smart Electronics", "Alan Doski", "+964 750 776 5432", "alan.doski@duhoktech.io", "Duhok"},
                {"Al-Mansour Luxury Perfumes", "Mustafa Al-Ani", "+964 780 445 9900", "mustafa.ani@mansourperfumes.iq", "Baghdad"},
                {"Basra Seafood Restaurant", "Karrar Al-Basri", "+964 781 223 6677", "karrar.basri@basragrill.com", "Basra"},
                {"Kirkuk Castle Roastery & Cafe", "Omed Jalal", "+964 772 889 3344", "omed.jalal@kirkukroast.com", "Kirkuk"},
                {"Hawler Boutique & Fashion", "Shanya Barzan", "+964 750 334 5566", "shanya.barzan@hawlerfashion.com", "Erbil"},
                {"Zakho Wholesale Hardware", "Choman Sindi", "+964 750 119 4488", "choman.sindi@zakhohardware.com", "Zakho"},
                {"Najaf Medical Supply Center", "Ali Al-Husseini", "+964 782 556 7711", "ali.husseini@najafmed.iq", "Najaf"},
                {"Karbala Heritage Hotel & Lounge", "Hassan Al-Sadr", "+964 780 998 1122", "hassan.sadr@karbalahotel.com", "Karbala"}
            };

            List<TimeFilterPreset> presets = timeFilterPresetRepository.findAllByOrderByIdAsc();

            for (int i = 0; i < customerDefs.length; i++) {
                Object[] def = customerDefs[i];
                String name = (String) def[0];
                String contact = (String) def[1];
                String phone = (String) def[2];
                String email = (String) def[3];
                String city = (String) def[4];

                Customer c = customerRepository.save(Customer.builder()
                        .name(name + " (" + contact + ")")
                        .phoneNumber(phone)
                        .email(email)
                        .city(city)
                        .totalOrders(4)
                        .totalSpent(0.0)
                        .status("FOLLOW_UP")
                        .build());

                // Define 4 orders distributed across 60 days
                int days1 = (i % 2 == 0) ? 0 : 1; // Today / 24h
                int days2 = 4 + (i * 2) % 6;       // 4-8 days ago
                int days3 = 14 + (i * 3) % 12;     // 14-25 days ago
                int days4 = 35 + (i * 4) % 22;     // 35-56 days ago

                int[] orderDays = {days1, days2, days3, days4};
                String[][] orderItemSets = {
                    {"Twekl Cloud POS Annual License", "Thermal Receipt Paper Box (x50 rolls)"},
                    {"Dual-Screen Touch Master Terminal", "Cash Drawer Heavy Duty 5-Bill 8-Coin"},
                    {"Wireless 2D QR Barcode Scanner", "High-Speed Kitchen Ticket Printer (LAN/WiFi)"},
                    {"Self-Checkout Kiosk 21-inch", "Omnidirectional Desktop Laser Scanner"},
                    {"Mobile Android POS Handheld", "Bluetooth Receipt Printer"},
                    {"Table Management & Kitchen Display System", "Thermal Rolls Pack x100"},
                    {"Pharmacy Barcode Matrix Scanners (x4)", "Touchscreen Display Stand Bracket"},
                    {"Heavy Duty Steel Cash Drawer", "High-Speed Barcode Label Printer"}
                };

                double runningSpent = 0;
                for (int j = 0; j < orderDays.length; j++) {
                    int d = orderDays[j];
                    LocalDateTime orderDate = (d == 0) ? now.minusHours(6 + (i * 2)) : now.minusDays(d).minusHours(3);
                    String[] items = orderItemSets[(i + j) % orderItemSets.length];
                    String summary = String.join(", ", items);
                    double amount = 220.0 + ((i + 1) * 65.0) + (j * 110.0);
                    runningSpent += amount;

                    String orderNum = "ORD-" + (1000 + (i * 10) + j);
                    CustomerOrder order = orderRepository.save(CustomerOrder.builder()
                            .customerId(c.getId())
                            .orderNumber(orderNum)
                            .orderDate(orderDate)
                            .itemsSummary(summary)
                            .totalAmount(amount)
                            .paymentMethod(j % 3 == 0 ? "FIB Cash" : (j % 3 == 1 ? "Credit Card" : "Cash on Delivery"))
                            .orderStatus("DELIVERED")
                            .build());

                    // Create milestone checks for this order
                    for (TimeFilterPreset p : presets) {
                        int durDays = "HOURS".equalsIgnoreCase(p.getDurationUnit()) ? 1 : (p.getDurationValue() != null ? p.getDurationValue() : 7);
                        boolean isDue = d >= durDays;
                        
                        // Realistic completion distribution
                        boolean isCompleted;
                        String sat = null;
                        String note;
                        String img = "";
                        String agent = (i % 2 == 0) ? "Agent Sarah" : "Agent Tariq";

                        if (d >= durDays + 5) {
                            isCompleted = true; // Completed well in past
                            sat = (j % 4 == 0) ? "SATISFIED" : (j % 4 == 1 ? "NEUTRAL" : (j % 4 == 2 ? "UNSATISFIED" : null));
                            if ("SATISFIED".equals(sat)) {
                                note = "Customer confirmed " + items[0] + " works perfectly! Cashier team trained in 1 hour.";
                            } else if ("NEUTRAL".equals(sat)) {
                                note = "Hardware is operational. Customer requested Kurdish font layout for receipt header.";
                            } else if ("UNSATISFIED".equals(sat)) {
                                note = "Initial bluetooth pairing dropped on scanner. Reconfigured and replaced cable under warranty.";
                            } else {
                                note = "Routine checkpoint call. Store manager verified all stations online.";
                            }
                            img = sampleProofImages[(i + j) % sampleProofImages.length];
                        } else if (d >= durDays) {
                            // Around the due date - some completed, some pending
                            isCompleted = (i + j) % 2 == 0;
                            if (isCompleted) {
                                sat = (j % 3 == 0) ? "SATISFIED" : "NEUTRAL";
                                note = "Customer follow-up completed. Expressed satisfaction with delivery timeline.";
                                img = sampleProofImages[(i + j) % sampleProofImages.length];
                            } else {
                                note = "Pending follow-up check call.";
                            }
                        } else {
                            isCompleted = false;
                            note = "Scheduled checkpoint. Due in upcoming cycle.";
                        }

                        followupCheckRepository.save(OrderFollowupCheck.builder()
                                .orderId(order.getId())
                                .presetId(p.getId())
                                .presetName(p.getName())
                                .durationValue(p.getDurationValue())
                                .durationUnit(p.getDurationUnit())
                                .isCompleted(isCompleted)
                                .satisfaction(sat)
                                .note(note)
                                .imageUrl(img)
                                .checkedBy(isCompleted ? agent : "")
                                .checkedAt(isCompleted ? orderDate.plusDays(durDays) : null)
                                .build());
                    }
                }

                c.setTotalSpent(runningSpent);
                c.setLastOrderDate(now.minusDays(orderDays[0]));
                c.setDaysSinceLastOrder(orderDays[0]);
                customerRepository.save(c);

                // Add Customer Feedback entry
                String fbSat = (i % 3 == 0) ? "SATISFIED" : (i % 3 == 1 ? "NEUTRAL" : "UNSATISFIED");
                String fbContent = "SATISFIED".equals(fbSat) ? 
                    "Excellent technical support and high quality hardware! Will recommend Twekl to our branch partners." :
                    ("NEUTRAL".equals(fbSat) ? "System working as expected, awaiting Kurdish documentation update." : 
                    "Slight delay during initial printer delivery, but follow-up agent resolved it quickly.");
                feedbackRepository.save(CustomerFeedback.builder()
                        .customerId(c.getId())
                        .customerName(c.getName())
                        .authorName(i % 2 == 0 ? "Agent Sarah" : "Agent Rezan")
                        .feedbackType(i % 2 == 0 ? "COMPLIMENT" : "NOTE")
                        .content(fbContent)
                        .satisfaction(fbSat)
                        .imageUrl(sampleProofImages[i % sampleProofImages.length])
                        .status("FOLLOWED_UP")
                        .createdAt(now.minusDays(orderDays[1]))
                        .build());
            }
        }

        // 5. Update any existing completed checks that have null satisfaction to have realistic satisfaction distribution
        List<OrderFollowupCheck> existingCompleted = followupCheckRepository.findAll();
        for (int k = 0; k < existingCompleted.size(); k++) {
            OrderFollowupCheck chk = existingCompleted.get(k);
            if (Boolean.TRUE.equals(chk.getIsCompleted()) && (chk.getSatisfaction() == null || chk.getSatisfaction().trim().isEmpty() || "BLANK".equalsIgnoreCase(chk.getSatisfaction()))) {
                if (k % 5 == 0) {
                    chk.setSatisfaction("SATISFIED");
                    if (chk.getNote() == null || chk.getNote().isEmpty()) chk.setNote("Customer highly satisfied with hardware performance.");
                } else if (k % 5 == 1) {
                    chk.setSatisfaction("SATISFIED");
                    if (chk.getNote() == null || chk.getNote().isEmpty()) chk.setNote("System operating smoothly with zero errors.");
                } else if (k % 5 == 2) {
                    chk.setSatisfaction("NEUTRAL");
                    if (chk.getNote() == null || chk.getNote().isEmpty()) chk.setNote("Operational. Client requested operational tips for end-of-day reports.");
                } else if (k % 5 == 3) {
                    chk.setSatisfaction("UNSATISFIED");
                    if (chk.getNote() == null || chk.getNote().isEmpty()) chk.setNote("Customer experienced paper jam on kitchen printer. Replaced roller.");
                } else {
                    chk.setSatisfaction(null); // Keep some genuinely blank / unrated
                }
                followupCheckRepository.save(chk);
            }
        }
        
        // 6. Ensure all customers have realistically spread order dates and synchronized stats
        List<Customer> allCusts = customerRepository.findAll();
        int[] baseDays = {0, 1, 3, 5, 7, 14, 22, 35, 45, 55};
        for (int i = 0; i < allCusts.size(); i++) {
            Customer cust = allCusts.get(i);
            List<CustomerOrder> custOrders = orderRepository.findByCustomerIdOrderByOrderDateDesc(cust.getId());
            if (!custOrders.isEmpty()) {
                if (cust.getId() >= 8) {
                    int bDay = baseDays[Math.abs(i - 7) % baseDays.length];
                    for (int j = 0; j < custOrders.size(); j++) {
                        CustomerOrder o = custOrders.get(j);
                        int orderDaysAgo = (j == 0) ? bDay : (bDay + 5 * (j + 1) + (i % 3));
                        LocalDateTime oDate = (orderDaysAgo == 0) ? now.minusHours(4 + (i * 2)) : now.minusDays(orderDaysAgo).minusHours(2);
                        o.setOrderDate(oDate);
                        orderRepository.save(o);
                    }
                }
                
                CustomerOrder latest = custOrders.get(0);
                cust.setLastOrderDate(latest.getOrderDate());
                long diff = ChronoUnit.DAYS.between(latest.getOrderDate().toLocalDate(), LocalDate.now());
                cust.setDaysSinceLastOrder((int) Math.max(0, diff));
                cust.setTotalOrders(custOrders.size());
                double sum = custOrders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
                cust.setTotalSpent(sum);
                customerRepository.save(cust);
            }
        }

        log.info("2-month rich operational data seeding and enrichment completed successfully!");
    }
}
