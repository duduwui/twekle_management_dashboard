package com.twekl.dashboard.administration.dto.admin;

import com.twekl.dashboard.administration.dto.admin.AdminPermissionDto;
import java.util.List;
import java.util.Set;

public class UpdateAdminRequest {

    private String phoneNumber;
    @com.fasterxml.jackson.annotation.JsonProperty("isSuperAdmin")
    private Boolean isSuperAdmin;
    private Boolean canCreateRoles;
    private Boolean canManageUsers;
    private Boolean active;
    private String password;
    private Set<Long> roleIds;
    private List<AdminPermissionDto> permissions;

    public UpdateAdminRequest() {}

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @com.fasterxml.jackson.annotation.JsonProperty("superAdmin")
    public void setSuperAdmin(Boolean superAdmin) {
        this.isSuperAdmin = superAdmin;
    }

    public Boolean getSuperAdmin() { return isSuperAdmin; }

    public Boolean getIsSuperAdmin() { return isSuperAdmin; }
    public void setIsSuperAdmin(Boolean superAdmin) { isSuperAdmin = superAdmin; }

    public Boolean getCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(Boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public Boolean getCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(Boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(Set<Long> roleIds) { this.roleIds = roleIds; }

    public List<AdminPermissionDto> getPermissions() { return permissions; }
    public void setPermissions(List<AdminPermissionDto> permissions) { this.permissions = permissions; }
}

