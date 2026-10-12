package com.twekl.dashboard.administration.service.admin;

import com.twekl.dashboard.administration.dto.admin.AdminPermissionDto;
import com.twekl.dashboard.administration.dto.admin.UpdateAdminRequest;
import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
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
public class AdminUpdateService {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUpdateService(AdminRepository adminRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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

    public Admin assignRolesToAdmin(Long adminId, Set<Long> roleIds) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
        admin.setRoles(roles);
        return adminRepository.save(admin);
    }
}
