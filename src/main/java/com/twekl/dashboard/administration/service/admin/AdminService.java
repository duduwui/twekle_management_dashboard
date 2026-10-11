package com.twekl.dashboard.administration.service.admin;

import com.twekl.dashboard.administration.model.customer.Customer;

import com.twekl.dashboard.administration.dto.admin.AdminPermissionDto;
import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.model.admin.AdminPermission;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.repository.admin.AdminRepository;
import com.twekl.dashboard.administration.repository.role.RoleRepository;
import com.twekl.dashboard.administration.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.administration.dto.admin.UpdateAdminRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findById(Long adminId) {
        return adminRepository.findById(adminId);
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByAdminId(Long adminId) {
        return adminRepository.findById(adminId);
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public long count() {
        return adminRepository.count();
    }

    public Admin createAdmin(CreateAdminRequest req) {
        if (adminRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username '" + req.getUsername() + "' is already taken");
        }

        Admin admin = Admin.builder()
                .username(req.getUsername().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .phoneNumber(req.getPhoneNumber())
                .isSuperAdmin(req.isSuperAdmin())
                .canCreateRoles(req.isCanCreateRoles())
                .canManageUsers(req.isCanManageUsers())
                .active(req.isActive())
                .build();

        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(req.getRoleIds()));
            admin.setRoles(roles);
        }

        if (req.getPermissions() != null && !req.getPermissions().isEmpty()) {
            for (AdminPermissionDto dto : req.getPermissions()) {
                AdminPermission p = new AdminPermission(admin, dto.getModuleKey(), dto.getModuleNameEn(),
                        dto.isCanCreate(), dto.isCanRead(), dto.isCanUpdate(), dto.isCanDelete());
                admin.getPermissions().add(p);
            }
        } else {
            seedDefaultPermissions(admin);
        }

        return adminRepository.save(admin);
    }

    public Admin updateAdmin(Long adminId, UpdateAdminRequest req) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));

        if (req.getPhoneNumber() != null) admin.setPhoneNumber(req.getPhoneNumber());
        if (req.getIsSuperAdmin() != null) admin.setSuperAdmin(req.getIsSuperAdmin());
        if (req.getCanCreateRoles() != null) admin.setCanCreateRoles(req.getCanCreateRoles());
        if (req.getCanManageUsers() != null) admin.setCanManageUsers(req.getCanManageUsers());
        if (req.getActive() != null) admin.setActive(req.getActive());
        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        }
        if (req.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(req.getRoleIds()));
            admin.setRoles(roles);
        }
        if (req.getPermissions() != null) {
            admin.getPermissions().clear();
            for (AdminPermissionDto dto : req.getPermissions()) {
                AdminPermission p = new AdminPermission(admin, dto.getModuleKey(), dto.getModuleNameEn(),
                        dto.isCanCreate(), dto.isCanRead(), dto.isCanUpdate(), dto.isCanDelete());
                admin.getPermissions().add(p);
            }
        }

        return adminRepository.save(admin);
    }

    public Admin toggleStatus(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));
        if ("twekl_super_admin".equalsIgnoreCase(admin.getUsername()) && admin.isActive()) {
            throw new IllegalArgumentException("Primary super administrator account cannot be deactivated");
        }
        admin.setActive(!admin.isActive());
        return adminRepository.save(admin);
    }

    public Admin assignRolesToAdmin(Long adminId, Set<Long> roleIds) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
        admin.setRoles(roles);
        return adminRepository.save(admin);
    }

    public void deleteAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));
        if ("twekl_super_admin".equalsIgnoreCase(admin.getUsername())) {
            throw new IllegalArgumentException("Primary super administrator account cannot be deleted");
        }
        adminRepository.delete(admin);
    }

    public void seedDefaultPermissions(Admin admin) {
        admin.getPermissions().clear();
        boolean isSuper = admin.isSuperAdmin();
        boolean canManage = isSuper || admin.isCanManageUsers();
        boolean canCreateR = isSuper || admin.isCanCreateRoles();

        boolean roleCreate = false;
        boolean roleRead = false;
        boolean roleUpdate = false;
        boolean roleDelete = false;

        if (isSuper) {
            roleCreate = true;
            roleRead = true;
            roleUpdate = true;
            roleDelete = true;
        } else if (admin.getRoles() != null && !admin.getRoles().isEmpty()) {
            roleCreate = admin.getRoles().stream().anyMatch(Role::isCanCreate);
            roleRead = admin.getRoles().stream().anyMatch(Role::isCanRead);
            roleUpdate = admin.getRoles().stream().anyMatch(Role::isCanUpdate);
            roleDelete = admin.getRoles().stream().anyMatch(Role::isCanDelete);
        }

        // 1. ADMIN_MANAGEMENT
        admin.getPermissions().add(new AdminPermission(admin, "ADMIN_MANAGEMENT", "Administration & Staff",
                isSuper || (canManage && canCreateR), isSuper || canManage || canCreateR, isSuper || canManage, isSuper));

        // 2. CUSTOMER_FOLLOWUP
        admin.getPermissions().add(new AdminPermission(admin, "CUSTOMER_FOLLOWUP", "Customer Follow-ups",
                isSuper || roleCreate, isSuper || roleRead, isSuper || roleUpdate, isSuper || roleDelete));

        // 3. REPORTS_INSIGHTS
        admin.getPermissions().add(new AdminPermission(admin, "REPORTS_INSIGHTS", "Reports & Analytics",
                isSuper || roleCreate, isSuper || roleRead, isSuper || roleUpdate, isSuper || roleDelete));
    }

    public boolean hasPermission(Admin admin, String moduleKey, String action) {
        if (admin == null || moduleKey == null || action == null) return false;
        if (admin.isSuperAdmin()) return true;
        if (admin.getPermissions() != null && !admin.getPermissions().isEmpty()) {
            for (AdminPermission p : admin.getPermissions()) {
                if (p.getModuleKey().equalsIgnoreCase(moduleKey)) {
                    return switch (action.toUpperCase()) {
                        case "CREATE" -> p.isCanCreate();
                        case "READ" -> p.isCanRead();
                        case "UPDATE" -> p.isCanUpdate();
                        case "DELETE" -> p.isCanDelete();
                        default -> false;
                    };
                }
            }
            return false;
        }
        if (admin.getRoles() != null && !admin.getRoles().isEmpty()) {
            return switch (action.toUpperCase()) {
                case "CREATE" -> admin.getRoles().stream().anyMatch(Role::isCanCreate);
                case "READ" -> admin.getRoles().stream().anyMatch(Role::isCanRead);
                case "UPDATE" -> admin.getRoles().stream().anyMatch(Role::isCanUpdate);
                case "DELETE" -> admin.getRoles().stream().anyMatch(Role::isCanDelete);
                default -> false;
            };
        }
        return false;
    }

    public boolean hasCurrentUserPermission(String moduleKey, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        boolean hasSuperRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (hasSuperRole) {
            return true;
        }
        return findByUsername(auth.getName())
                .map(admin -> hasPermission(admin, moduleKey, action))
                .orElse(false);
    }

    public Optional<Admin> getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return findByUsername(auth.getName());
    }
}

