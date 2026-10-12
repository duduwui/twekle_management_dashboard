package com.twekl.dashboard.service.administrative;

import com.twekl.dashboard.dto.administrative.AdminPermissionDto;
import com.twekl.dashboard.dto.administrative.CreateAdminRequest;
import com.twekl.dashboard.dto.administrative.UpdateAdminRequest;
import com.twekl.dashboard.exception.administrative.ResourceNotFoundException;
import com.twekl.dashboard.model.administrative.Admin;
import com.twekl.dashboard.model.administrative.AdminPermission;
import com.twekl.dashboard.model.administrative.Role;
import com.twekl.dashboard.repository.administrative.AdminRepository;
import com.twekl.dashboard.repository.administrative.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for staff administrators and permission management.
 */
@Service
@Transactional
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminService() {
    }

    @Autowired
    public AdminService(AdminRepository adminRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder) {
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

    @Transactional(readOnly = true)
    public Optional<Admin> getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return findByUsername(auth.getName());
    }

    @Transactional(readOnly = true)
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
        }
        if (admin.getRoles() != null && !admin.getRoles().isEmpty()) {
            for (Role role : admin.getRoles()) {
                boolean allowed = switch (action.toUpperCase()) {
                    case "CREATE" -> role.isCanCreate();
                    case "READ" -> role.isCanRead();
                    case "UPDATE" -> role.isCanUpdate();
                    case "DELETE" -> role.isCanDelete();
                    default -> false;
                };
                if (allowed) return true;
            }
        }
        return false;
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

        admin.getPermissions().add(new AdminPermission(admin, "ADMIN_MANAGEMENT", "Administration & Staff",
                canManage, canManage, canManage, canManage));
        admin.getPermissions().add(new AdminPermission(admin, "ROLE_MANAGEMENT", "Roles & Permissions",
                canCreateR, canCreateR, canCreateR, canCreateR));
        admin.getPermissions().add(new AdminPermission(admin, "CUSTOMER_FOLLOWUP", "Customer Follow-ups",
                true, true, true, isSuper));
        admin.getPermissions().add(new AdminPermission(admin, "TIMELINES_PRESETS", "Time Filters & Presets",
                isSuper, true, isSuper, isSuper));
        admin.getPermissions().add(new AdminPermission(admin, "REPORTS_INSIGHTS", "Reports & Analytics",
                true, true, true, false));
    }
}
