package com.twekl.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateAdminDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain alphanumeric characters, dots, underscores, and dashes")
    private String username;

    private String password; // optional on update

    @Pattern(regexp = "^(\\+?[0-9]{7,15})?$", message = "Invalid phone number format")
    private String phoneNumber;

    private Boolean isSuperAdmin;
    private Boolean canCreateRoles;
    private Boolean canManageUsers;
    private String status;

    public UpdateAdminDto() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Boolean getIsSuperAdmin() { return isSuperAdmin; }
    public void setIsSuperAdmin(Boolean superAdmin) { isSuperAdmin = superAdmin; }

    public Boolean getCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(Boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public Boolean getCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(Boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
