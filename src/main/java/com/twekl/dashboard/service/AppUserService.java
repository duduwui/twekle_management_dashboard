package com.twekl.dashboard.service;

import com.twekl.dashboard.model.AppUser;
import com.twekl.dashboard.model.ModulePermission;
import com.twekl.dashboard.repository.AppUserRepository;
import com.twekl.dashboard.repository.ModulePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private final ModulePermissionRepository permissionRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    public AppUserService(AppUserRepository userRepository, 
                          ModulePermissionRepository permissionRepository,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<AppUser> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<AppUser> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(query.trim());
    }

    @Transactional
    public AppUser createUser(AppUser user, List<ModulePermission> initialModules) {
        if (userRepository.existsByUsernameEn(user.getUsernameEn())) {
            throw new IllegalArgumentException("Username '" + user.getUsernameEn() + "' already exists");
        }
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        }
        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("ACTIVE");
        }

        AppUser savedUser = userRepository.save(user);

        // If no custom initial modules provided, seed standard 3-card modules
        if (initialModules == null || initialModules.isEmpty()) {
            initialModules = getDefaultModuleCatalog(savedUser.getId());
        } else {
            for (ModulePermission mp : initialModules) {
                mp.setUserId(savedUser.getId());
            }
        }

        permissionRepository.saveAll(initialModules);
        return savedUser;
    }

    public List<ModulePermission> getPermissionsForUser(Long userId) {
        List<ModulePermission> permissions = permissionRepository.findByUserId(userId);
        if (permissions.isEmpty()) {
            // Lazy seed standard modules if missing
            List<ModulePermission> defaults = getDefaultModuleCatalog(userId);
            return permissionRepository.saveAll(defaults);
        }
        return permissions;
    }

    @Transactional
    public ModulePermission updateModulePermission(Long userId, String moduleKey, String field, boolean value) {
        ModulePermission perm = permissionRepository.findByUserIdAndModuleKey(userId, moduleKey)
                .orElseGet(() -> {
                    ModulePermission newPerm = ModulePermission.builder()
                            .userId(userId)
                            .moduleKey(moduleKey)
                            .moduleNameEn(moduleKey)
                            .build();
                    return permissionRepository.save(newPerm);
                });

        switch (field.toLowerCase()) {
            case "create":
            case "cancreate":
            case "c":
                perm.setCanCreate(value);
                break;
            case "read":
            case "canread":
            case "r":
                perm.setCanRead(value);
                break;
            case "update":
            case "canupdate":
            case "u":
                perm.setCanUpdate(value);
                break;
            case "delete":
            case "candelete":
            case "d":
                perm.setCanDelete(value);
                break;
            case "visible":
            case "isvisible":
            case "visibility":
                perm.setVisible(value);
                break;
            case "full_crud":
                perm.setCanCreate(value);
                perm.setCanRead(value);
                perm.setCanUpdate(value);
                perm.setCanDelete(value);
                break;
            default:
                throw new IllegalArgumentException("Unknown permission field: " + field);
        }

        return permissionRepository.save(perm);
    }

    @Transactional
    public ModulePermission addCustomModuleForUser(Long userId, ModulePermission module) {
        module.setUserId(userId);
        return permissionRepository.save(module);
    }

    @Transactional
    public AppUser toggleUserStatus(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("INACTIVE");
        } else {
            user.setStatus("ACTIVE");
        }
        return userRepository.save(user);
    }

    @Transactional
    public AppUser updateUser(Long id, AppUser updated) {
        AppUser existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        if (updated.getUsernameEn() != null && !updated.getUsernameEn().trim().isEmpty()) {
            if (!existing.getUsernameEn().equals(updated.getUsernameEn()) && userRepository.existsByUsernameEn(updated.getUsernameEn())) {
                throw new IllegalArgumentException("Username '" + updated.getUsernameEn() + "' already exists");
            }
            existing.setUsernameEn(updated.getUsernameEn());
        }
        if (updated.getUsernameAr() != null) {
            existing.setUsernameAr(updated.getUsernameAr());
        }
        if (updated.getUsernameKu() != null) {
            existing.setUsernameKu(updated.getUsernameKu());
        }
        if (updated.getPhoneNumber() != null) {
            existing.setPhoneNumber(updated.getPhoneNumber());
        }
        if (updated.getPassword() != null && !updated.getPassword().trim().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updated.getPassword().trim()));
        }
        if (updated.getStatus() != null && !updated.getStatus().trim().isEmpty()) {
            existing.setStatus(updated.getStatus());
        }
        return userRepository.save(existing);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        permissionRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    public List<ModulePermission> getDefaultModuleCatalog(Long userId) {
        List<ModulePermission> list = new ArrayList<>();

        // Card 1: Software Development
        list.add(ModulePermission.builder()
                .userId(userId)
                .moduleKey("SOFTWARE")
                .moduleNameEn("Software & Development")
                .moduleNameAr("تطوير البرمجيات والأنظمة")
                .moduleNameKu("پەرەپێدانی سۆفتوێر")
                .descriptionEn("Source code repositories, deployments, API services, and dev tools")
                .descriptionAr("مستودعات الكود، خدمات الواجهات البرمجية، وأدوات المطورين")
                .descriptionKu("کۆگاکانی کۆد، خزمەتگوزاریەکانی API، و ئامرازەکانی پەرەپێدەران")
                .canCreate(false)
                .canRead(true)
                .canUpdate(false)
                .canDelete(false)
                .isVisible(true)
                .badgeColor("#4F46E5")
                .build());

        // Card 2: Sales & Revenue
        list.add(ModulePermission.builder()
                .userId(userId)
                .moduleKey("SALES")
                .moduleNameEn("Sales & Revenue")
                .moduleNameAr("المبيعات والإيرادات")
                .moduleNameKu("فرۆشتن و داهات")
                .descriptionEn("Invoices, transaction pipeline, client contracts, and financial reports")
                .descriptionAr("الفواتير، خط المبيعات، عقود العملاء، والتقارير المالية")
                .descriptionKu("پسوولەکان، گرێبەستی کڕیاران، و ڕاپۆرتە داراییەکان")
                .canCreate(true)
                .canRead(true)
                .canUpdate(true)
                .canDelete(true)
                .isVisible(true)
                .badgeColor("#35B89F")
                .build());

        // Card 3: Product Management
        list.add(ModulePermission.builder()
                .userId(userId)
                .moduleKey("PRODUCT")
                .moduleNameEn("Product Management")
                .moduleNameAr("إدارة المنتجات")
                .moduleNameKu("بەڕێوەبردنی بەرهەمەکان")
                .descriptionEn("Product catalogs, inventory levels, release roadmaps, and feature logs")
                .descriptionAr("كتالوج المنتجات، مستويات المخزون، وخطط إطلاق الميزات")
                .descriptionKu("کاتالۆگی بەرهەمەکان، ئاستی مەخزەن، و نەخشەی کار")
                .canCreate(false)
                .canRead(true)
                .canUpdate(true)
                .canDelete(false)
                .isVisible(true)
                .badgeColor("#0EA5E9")
                .build());

        return list;
    }
}
