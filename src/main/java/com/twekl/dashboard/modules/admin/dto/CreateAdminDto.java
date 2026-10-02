package com.twekl.dashboard.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateAdminDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password must be at least 6 characters")
    private String password;

    private String phoneNumber;
    private boolean isSuperAdmin = false;
    private boolean canCreateRoles = true;
    private boolean canManageUsers = true;
    private String status = "ACTIVE";

    public CreateAdminDto() {}

    public CreateAdminDto(String username, String password, String phoneNumber, boolean isSuperAdmin, boolean canCreateRoles, boolean canManageUsers, String status) {
        this.username = username;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.isSuperAdmin = isSuperAdmin;
        this.canCreateRoles = canCreateRoles;
        this.canManageUsers = canManageUsers;
        this.status = status;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean isSuperAdmin) { this.isSuperAdmin = isSuperAdmin; }

    public boolean isCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public boolean isCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
