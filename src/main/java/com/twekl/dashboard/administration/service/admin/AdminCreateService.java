package com.twekl.dashboard.administration.service.admin;

import com.twekl.dashboard.administration.dto.admin.AdminPermissionDto;
import com.twekl.dashboard.administration.dto.admin.CreateAdminRequest;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.model.admin.AdminPermission;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.repository.admin.AdminRepository;
import com.twekl.dashboard.administration.repository.role.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class AdminCreateService {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminCreateService(AdminRepository adminRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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
}
