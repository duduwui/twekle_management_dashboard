package com.twekl.dashboard.modules.admin.service;

import com.twekl.dashboard.modules.admin.dto.CreateAdminDto;
import com.twekl.dashboard.modules.admin.dto.UpdateAdminDto;
import com.twekl.dashboard.modules.admin.entity.Admin;
import com.twekl.dashboard.modules.admin.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    public Admin createAdmin(CreateAdminDto dto) {
        if (adminRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken");
        }

        Admin admin = Admin.builder()
                .username(dto.getUsername().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(dto.getPhoneNumber())
                .isSuperAdmin(dto.isSuperAdmin())
                .canCreateRoles(dto.isCanCreateRoles())
                .canManageUsers(dto.isCanManageUsers())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        return adminRepository.save(admin);
    }

    public Admin updateAdmin(Long id, UpdateAdminDto dto) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found with id: " + id));

        if (dto.getUsername() != null && !dto.getUsername().trim().isEmpty()) {
            String newUsername = dto.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(admin.getUsername()) && adminRepository.existsByUsername(newUsername)) {
                throw new IllegalArgumentException("Username '" + newUsername + "' is already taken");
            }
            admin.setUsername(newUsername);
        }

        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getPhoneNumber() != null) {
            admin.setPhoneNumber(dto.getPhoneNumber());
        }

        if (dto.getIsSuperAdmin() != null) {
            admin.setSuperAdmin(dto.getIsSuperAdmin());
        }

        if (dto.getCanCreateRoles() != null) {
            admin.setCanCreateRoles(dto.getCanCreateRoles());
        }

        if (dto.getCanManageUsers() != null) {
            admin.setCanManageUsers(dto.getCanManageUsers());
        }

        if (dto.getStatus() != null) {
            admin.setStatus(dto.getStatus());
        }

        return adminRepository.save(admin);
    }

    public void deleteAdmin(Long id) {
        if (!adminRepository.existsById(id)) {
            throw new IllegalArgumentException("Admin not found with id: " + id);
        }
        adminRepository.deleteById(id);
    }

    public long count() {
        return adminRepository.count();
    }
}
