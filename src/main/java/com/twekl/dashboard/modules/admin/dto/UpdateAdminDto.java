package com.twekl.dashboard.modules.admin.dto;

import jakarta.validation.constraints.Size;

public class UpdateAdminDto {

    @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
    private String username;

    @Size(min = 6, max = 128, message = "Password must be at least 6 characters")
    private String password;

    private String phoneNumber;
    private Boolean isSuperAdmin;
    private Boolean canCreateRoles;
    private Boolean canManageUsers;
    private String status;

    public UpdateAdminDto() {}

    public UpdateAdminDto(String username, String password, String phoneNumber, Boolean isSuperAdmin, Boolean canCreateRoles, Boolean canManageUsers, String status) {
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

    public Boolean getIsSuperAdmin() { return isSuperAdmin; }
    public void setIsSuperAdmin(Boolean isSuperAdmin) { this.isSuperAdmin = isSuperAdmin; }

    public Boolean getCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(Boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public Boolean getCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(Boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
