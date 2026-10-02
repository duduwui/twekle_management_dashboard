package com.twekl.dashboard.modules.admin.controller;

import com.twekl.dashboard.common.dto.ApiResponse;
import com.twekl.dashboard.modules.admin.dto.CreateAdminDto;
import com.twekl.dashboard.modules.admin.dto.UpdateAdminDto;
import com.twekl.dashboard.modules.admin.entity.Admin;
import com.twekl.dashboard.modules.admin.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/internal/admins", "/api/admins"})
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<Admin>> getAllAdmins() {
        return ResponseEntity.ok(adminService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Admin> getAdminById(@PathVariable Long id) {
        return adminService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Admin>> createAdmin(@Valid @RequestBody CreateAdminDto dto) {
        Admin created = adminService.createAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Admin created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Admin>> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminDto dto) {
        Admin updated = adminService.updateAdmin(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Admin updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Admin deleted successfully", null));
    }
}
