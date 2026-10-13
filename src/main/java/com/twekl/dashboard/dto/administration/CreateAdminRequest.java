package com.twekl.dashboard.dto.administration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateAdminRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Size(max = 32, message = "Phone number must not exceed 32 characters")
    private String phoneNumber;

    @JsonProperty("isSuperAdmin")
    @JsonAlias({"superAdmin"})
    private boolean superAdmin = false;

    private boolean canCreateRoles = false;
    private boolean canManageUsers = false;
    private boolean active = true;
    private Set<Long> roleIds = new HashSet<>();
    private List<AdminPermissionDto> permissions;

    public CreateAdminRequest() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public void setIsSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public boolean isCanCreateRoles() {
        return canCreateRoles;
    }

    public boolean canCreateRoles() {
        return canCreateRoles;
    }

    public void setCanCreateRoles(boolean canCreateRoles) {
        this.canCreateRoles = canCreateRoles;
    }

    public boolean isCanManageUsers() {
        return canManageUsers;
    }

    public boolean canManageUsers() {
        return canManageUsers;
    }

    public void setCanManageUsers(boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Set<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public List<AdminPermissionDto> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<AdminPermissionDto> permissions) {
        this.permissions = permissions;
    }
}
