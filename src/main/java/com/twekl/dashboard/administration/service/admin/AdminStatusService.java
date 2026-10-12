package com.twekl.dashboard.administration.service.admin;

import com.twekl.dashboard.administration.exception.ResourceNotFoundException;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.repository.admin.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminStatusService {

    private final AdminRepository adminRepository;

    public AdminStatusService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
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
}
