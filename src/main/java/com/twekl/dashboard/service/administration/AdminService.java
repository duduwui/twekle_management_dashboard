package com.twekl.dashboard.service.administration;

import com.twekl.dashboard.dto.administration.CreateAdminRequest;
import com.twekl.dashboard.dto.administration.UpdateAdminRequest;
import com.twekl.dashboard.model.administration.Admin;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for staff administrators and permission management.
 * Summarizes administrative account, permission checking, and lifecycle operations.
 */
public interface AdminService {

    List<Admin> findAll();

    Optional<Admin> findById(Long adminId);

    Optional<Admin> findByAdminId(Long adminId);

    Optional<Admin> findByUsername(String username);

    long count();

    Optional<Admin> getCurrentAdmin();

    boolean hasPermission(Admin admin, String moduleKey, String action);

    Admin createAdmin(CreateAdminRequest req);

    Admin updateAdmin(Long adminId, UpdateAdminRequest req);

    Admin toggleStatus(Long adminId);

    void deleteAdmin(Long adminId);

    void seedDefaultPermissions(Admin admin);
}
