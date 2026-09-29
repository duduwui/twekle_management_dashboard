package com.twekl.dashboard.service;

import com.twekl.dashboard.model.Admin;
import com.twekl.dashboard.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    public AdminService(AdminRepository adminRepository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    public Optional<Admin> getAdminById(Long id) {
        return adminRepository.findById(id);
    }

    public List<Admin> searchAdmins(String query) {
        if (query == null || query.trim().isEmpty()) {
            return adminRepository.findAll();
        }
        return adminRepository.searchAdmins(query.trim());
    }

    @Transactional
    public Admin createAdmin(Admin admin) {
        if (adminRepository.existsByUsername(admin.getUsername())) {
            throw new IllegalArgumentException("Admin username '" + admin.getUsername() + "' already exists");
        }
        if (admin.getPassword() != null && !admin.getPassword().trim().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword().trim()));
        }
        if (admin.getStatus() == null || admin.getStatus().trim().isEmpty()) {
            admin.setStatus("ACTIVE");
        }
        return adminRepository.save(admin);
    }

    @Transactional
    public Admin updateAdmin(Long id, Admin updated) {
        Admin existing = adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found with id: " + id));

        if (adminRepository.existsByUsernameAndIdNot(updated.getUsername(), id)) {
            throw new IllegalArgumentException("Username '" + updated.getUsername() + "' is already in use");
        }

        existing.setUsername(updated.getUsername());
        if (updated.getPassword() != null && !updated.getPassword().trim().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updated.getPassword().trim()));
        }
        existing.setPhoneNumber(updated.getPhoneNumber());
        existing.setSuperAdmin(updated.isSuperAdmin());
        existing.setCanCreateRoles(updated.isCanCreateRoles());
        existing.setCanManageUsers(updated.isCanManageUsers());

        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }

        return adminRepository.save(existing);
    }

    @Transactional
    public Admin toggleAdminStatus(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found with id: " + id));
        if ("ACTIVE".equalsIgnoreCase(admin.getStatus())) {
            admin.setStatus("INACTIVE");
        } else {
            admin.setStatus("ACTIVE");
        }
        return adminRepository.save(admin);
    }

    @Transactional
    public void deleteAdmin(Long id) {
        if (!adminRepository.existsById(id)) {
            throw new IllegalArgumentException("Admin not found with id: " + id);
        }
        adminRepository.deleteById(id);
    }
}
