package com.twekl.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateAdminDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain alphanumeric characters, dots, underscores, and dashes")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 64, message = "Password must be at least 6 characters long")
    private String password;

    @Pattern(regexp = "^(\\+?[0-9]{7,15})?$", message = "Invalid phone number format")
    private String phoneNumber;

    private boolean isSuperAdmin = false;
    private boolean canCreateRoles = true;
    private boolean canManageUsers = true;

    private String status = "ACTIVE";

    public CreateAdminDto() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }

    public boolean isCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public boolean isCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
