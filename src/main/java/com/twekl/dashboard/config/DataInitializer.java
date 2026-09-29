package com.twekl.dashboard.config;

import com.twekl.dashboard.model.*;
import com.twekl.dashboard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
        if (adminRepository.count() == 0) {
            log.info("Seeding Super Admin and Administrators...");
            adminRepository.save(Admin.builder()
                    .username("twekl_super_admin")
                    .password(passwordEncoder.encode("Super@2026"))
                    .phoneNumber("+966 50 111 2233")
                    .isSuperAdmin(true)
                    .canCreateRoles(true)
                    .canManageUsers(true)
                    .status("ACTIVE")
                    .build());

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

            // Customer 1: Dara Ahmed (24 Hours Ago - 1 day)
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

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c1.getId())
                    .orderNumber("ORD-801")
                    .orderDate(now.minusHours(24))
                    .itemsSummary("Twekl POS Cloud Subscription + Wireless Barcode Scanner")
                    .totalAmount(450.0)
                    .paymentMethod("FIB Cash")
                    .orderStatus("DELIVERED")
                    .build());

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c1.getId())
                    .orderNumber("ORD-720")
                    .orderDate(now.minusDays(18))
                    .itemsSummary("Thermal Receipt Paper Box (x50 rolls)")
                    .totalAmount(120.0)
                    .paymentMethod("Cash on Delivery")
                    .orderStatus("DELIVERED")
                    .build());

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c1.getId())
                    .orderNumber("ORD-650")
                    .orderDate(now.minusDays(45))
                    .itemsSummary("Touchscreen Display Stand Bracket")
                    .totalAmount(280.0)
                    .paymentMethod("Credit Card")
                    .orderStatus("DELIVERED")
                    .build());

            feedbackRepository.save(CustomerFeedback.builder()
                    .customerId(c1.getId())
                    .authorName("Agent Tariq")
                    .feedbackType("COMPLIMENT")
                    .content("Customer called expressing great appreciation for the quick 2-hour onboarding process! Requested extra thermal paper in next cycle.")
                    .rating(5)
                    .status("FOLLOWED_UP")
                    .createdAt(now.minusHours(20))
                    .build());

            // Customer 2: Layla Al-Khatib (24 Hours Ago - 1 day)
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

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c2.getId())
                    .orderNumber("ORD-805")
                    .orderDate(now.minusHours(23))
                    .itemsSummary("Inventory Handheld Scanner Bundle (2 Units)")
                    .totalAmount(340.0)
                    .paymentMethod("ZainCash")
                    .orderStatus("DELIVERED")
                    .build());

            feedbackRepository.save(CustomerFeedback.builder()
                    .customerId(c2.getId())
                    .authorName("Ops Super Admin")
                    .feedbackType("COMPLIMENT")
                    .content("First-time customer compliment: 'The courier delivery to Mansour was lightning fast and items were well packaged.'")
                    .rating(5)
                    .status("FOLLOWED_UP")
                    .createdAt(now.minusHours(18))
                    .build());

            // Customer 3: Soran Hawrami (7 Days Ago - 1 week)
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

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c3.getId())
                    .orderNumber("ORD-780")
                    .orderDate(now.minusDays(7))
                    .itemsSummary("POS Dual-Screen Android Terminal (Master Station)")
                    .totalAmount(890.0)
                    .paymentMethod("FIB Cash")
                    .orderStatus("DELIVERED")
                    .build());

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c3.getId())
                    .orderNumber("ORD-710")
                    .orderDate(now.minusDays(22))
                    .itemsSummary("Thermal Kitchen Ticket Printer (LAN / WiFi)")
                    .totalAmount(320.0)
                    .paymentMethod("FIB Cash")
                    .orderStatus("DELIVERED")
                    .build());

            feedbackRepository.save(CustomerFeedback.builder()
                    .customerId(c3.getId())
                    .authorName("Agent Sara")
                    .feedbackType("NOTE")
                    .content("7-Day follow-up checkup completed: Terminal is functioning smoothly in their Saholaka branch. Scheduled quarterly hardware maintenance.")
                    .rating(5)
                    .status("FOLLOWED_UP")
                    .createdAt(now.minusDays(6))
                    .build());

            // Customer 4: Zana Rashid (14 Days Ago - 2 weeks)
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

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c4.getId())
                    .orderNumber("ORD-775")
                    .orderDate(now.minusDays(7))
                    .itemsSummary("Twekl Retail POS Annual License Renewal")
                    .totalAmount(520.0)
                    .paymentMethod("Credit Card")
                    .orderStatus("DELIVERED")
                    .build());

            feedbackRepository.save(CustomerFeedback.builder()
                    .customerId(c4.getId())
                    .authorName("Agent Tariq")
                    .feedbackType("REVIEW")
                    .content("Customer feedback: 'Software is very fast compared to old desktop systems. Staff learned it in 15 minutes.'")
                    .rating(5)
                    .status("RESOLVED")
                    .createdAt(now.minusDays(5))
                    .build());

            // Customer 5: Rebwar Barzan (32 Days Ago - 30+ Days Dormant)
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

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c5.getId())
                    .orderNumber("ORD-690")
                    .orderDate(now.minusDays(32))
                    .itemsSummary("Full Restaurant POS Multi-Terminal Hardware Set (3 Stations)")
                    .totalAmount(2200.0)
                    .paymentMethod("Bank Transfer")
                    .orderStatus("DELIVERED")
                    .build());

            feedbackRepository.save(CustomerFeedback.builder()
                    .customerId(c5.getId())
                    .authorName("Ops Super Admin")
                    .feedbackType("NOTE")
                    .content("30+ Day Re-engagement alert: Client has been dormant for 32 days. Propose sending special 15% discount for their second branch expansion.")
                    .rating(4)
                    .status("PENDING")
                    .createdAt(now.minusDays(2))
                    .build());

            // Customer 6: Sara Mohammed (45 Days Ago - 30+ Days Dormant)
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

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c6.getId())
                    .orderNumber("ORD-620")
                    .orderDate(now.minusDays(45))
                    .itemsSummary("Pharmacy Barcode Matrix Scanners (x4)")
                    .totalAmount(760.0)
                    .paymentMethod("ZainCash")
                    .orderStatus("DELIVERED")
                    .build());

            feedbackRepository.save(CustomerFeedback.builder()
                    .customerId(c6.getId())
                    .authorName("Agent Tariq")
                    .feedbackType("INQUIRY")
                    .content("Customer inquired about automated expiry date tracking module. Follow-up scheduled for next week.")
                    .rating(4)
                    .status("PENDING")
                    .createdAt(now.minusDays(10))
                    .build());

            // Customer 7: John Smith (Active / Follow-up 24h)
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

            orderRepository.save(CustomerOrder.builder()
                    .customerId(c7.getId())
                    .orderNumber("ORD-990")
                    .orderDate(now.minusHours(4))
                    .itemsSummary("Cloud POS Enterprise Master Station + 4 Barcode Readers")
                    .totalAmount(2850.0)
                    .paymentMethod("Credit Card")
                    .orderStatus("DELIVERED")
                    .build());

            feedbackRepository.save(CustomerFeedback.builder()
                    .customerId(c7.getId())
                    .authorName("Agent John")
                    .feedbackType("COMPLIMENT")
                    .content("Client thrilled with system onboarding and fast responses.")
                    .rating(5)
                    .status("FOLLOWED_UP")
                    .createdAt(now.minusHours(2))
                    .build());
        }

        if (followupCheckRepository.count() == 0) {
            log.info("Seeding Initial Order Follow-up Milestone Checks & Notes...");
            LocalDateTime now = LocalDateTime.now();
            List<CustomerOrder> allOrders = orderRepository.findAll();
            List<TimeFilterPreset> presets = timeFilterPresetRepository.findAllByOrderByIdAsc();

            for (CustomerOrder o : allOrders) {
                for (TimeFilterPreset p : presets) {
                    boolean is24h = p.getName().toLowerCase().contains("24") || (p.getDurationValue() == 24 && "HOURS".equalsIgnoreCase(p.getDurationUnit()));
                    boolean is7d = p.getName().toLowerCase().contains("7") || (p.getDurationValue() == 7 && "DAYS".equalsIgnoreCase(p.getDurationUnit()));
                    
                    boolean completed = is24h || (is7d && o.getId() % 2 == 0);
                    String note = completed ? "Follow-up completed with client regarding " + o.getItemsSummary() + ". Equipment operational." : "Scheduled milestone check pending.";
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
    }
}
