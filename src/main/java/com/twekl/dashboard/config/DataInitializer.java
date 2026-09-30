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
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    public DataInitializer(AdminRepository adminRepository, 
                           RoleRepository roleRepository,
                           TimeFilterPresetRepository timeFilterPresetRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.timeFilterPresetRepository = timeFilterPresetRepository;
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

        log.info("System initialization complete. Super admin, role templates, and time filters active. No mock operational data seeded.");
    }
}
