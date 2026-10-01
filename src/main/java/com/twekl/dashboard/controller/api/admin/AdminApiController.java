package com.twekl.dashboard.controller.api.admin;

import com.twekl.dashboard.dto.CreateAdminDto;
import com.twekl.dashboard.dto.UpdateAdminDto;
import com.twekl.dashboard.model.Admin;
import com.twekl.dashboard.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admins", description = "Super Administrator account operations, search, role delegations, and lifecycle management")
@RestController
@RequestMapping("/api/admins")
public class AdminApiController {

    private final AdminService adminService;

    @Autowired
    public AdminApiController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "List all admins", description = "Retrieves all administrators with optional search keyword filtering")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<Admin>> getAllAdmins(@RequestParam(value = "query", required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return ResponseEntity.ok(adminService.searchAdmins(query));
        }
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @Operation(summary = "Get admin by ID", description = "Retrieves an administrator's profile details")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Admin> getAdminById(@PathVariable Long id) {
        return adminService.getAdminById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create admin", description = "Provisions a new administrator account (Super Admin only)")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Admin> createAdmin(@Valid @RequestBody CreateAdminDto dto) {
        Admin admin = Admin.builder()
                .username(dto.getUsername().trim())
                .password(dto.getPassword().trim())
                .phoneNumber(dto.getPhoneNumber())
                .isSuperAdmin(dto.isSuperAdmin())
                .canCreateRoles(dto.isCanCreateRoles())
                .canManageUsers(dto.isCanManageUsers())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        Admin created = adminService.createAdmin(admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update admin", description = "Modifies administrator settings, contact info, and status")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Admin> updateAdmin(@PathVariable Long id, @Valid @RequestBody UpdateAdminDto dto) {
        Admin admin = new Admin();
        admin.setUsername(dto.getUsername().trim());
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            admin.setPassword(dto.getPassword().trim());
        }
        admin.setPhoneNumber(dto.getPhoneNumber());
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

        Admin updated = adminService.updateAdmin(id, admin);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Toggle admin active status", description = "Switches between ACTIVE and INACTIVE state")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Admin> toggleStatus(@PathVariable Long id) {
        Admin toggled = adminService.toggleAdminStatus(id);
        return ResponseEntity.ok(toggled);
    }

    @Operation(summary = "Delete admin", description = "Permanently removes an administrator account")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Admin deleted successfully",
                "id", id
        ));
    }
}
