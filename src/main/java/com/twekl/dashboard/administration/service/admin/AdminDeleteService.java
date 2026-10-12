package com.twekl.dashboard.administration.service.admin;

import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.repository.admin.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminDeleteService {

    private final AdminRepository adminRepository;

    public AdminDeleteService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    public void deleteAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + adminId));
        if ("twekl_super_admin".equalsIgnoreCase(admin.getUsername())) {
            throw new IllegalArgumentException("Primary super administrator account cannot be deleted");
        }
        adminRepository.delete(admin);
    }
}
