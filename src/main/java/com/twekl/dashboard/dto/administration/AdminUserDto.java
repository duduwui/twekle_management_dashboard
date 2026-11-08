package com.twekl.dashboard.dto.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.twekl.dashboard.model.administration.Admin;
import com.twekl.dashboard.model.administration.AdminPermission;
import com.twekl.dashboard.model.administration.Role;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing administrative users and their assigned roles/permissions.
 * Strictly omits sensitive credentials like passwords while providing comprehensive metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserDto {

    private Long id;
    private Long adminId;
    private String username;
    private String phoneNumber;

    @JsonProperty("isSuperAdmin")
    private boolean superAdmin;

    private boolean active;
    private boolean canCreateRoles;
    private boolean canManageUsers;

    private String primaryRole;
    private List<String> roleNames = new ArrayList<>();
    private List<RoleDto> roles = new ArrayList<>();
    private List<AdminPermissionDto> permissions = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public AdminUserDto() {}

    public static AdminUserDto fromEntity(Admin admin) {
        if (admin == null) return null;
        AdminUserDto dto = new AdminUserDto();
        dto.setId(admin.getId());
        dto.setAdminId(admin.getId());
        dto.setUsername(admin.getUsername());
        dto.setPhoneNumber(admin.getPhoneNumber());
        dto.setSuperAdmin(admin.isSuperAdmin());
        dto.setActive(admin.isActive());
        dto.setCanCreateRoles(admin.isCanCreateRoles());
        dto.setCanManageUsers(admin.isCanManageUsers());
        dto.setCreatedAt(admin.getCreatedAt());
        dto.setUpdatedAt(admin.getUpdatedAt());

        List<RoleDto> roleDtos = new ArrayList<>();
        List<String> rNames = new ArrayList<>();
        if (admin.getRoles() != null) {
            for (Role r : admin.getRoles()) {
                RoleDto rd = new RoleDto(r.getId(), r.getName(), r.isCanCreate(), r.isCanRead(), r.isCanUpdate(), r.isCanDelete(), r.getActions());
                roleDtos.add(rd);
                if (r.getName() != null && !r.getName().trim().isEmpty()) {
                    rNames.add(r.getName().trim());
                }
            }
        }
        dto.setRoles(roleDtos);
        dto.setRoleNames(rNames);

        if (!rNames.isEmpty()) {
            dto.setPrimaryRole(String.join(", ", rNames));
        } else if (admin.isSuperAdmin()) {
            dto.setPrimaryRole("Super Admin");
        } else {
            dto.setPrimaryRole("Staff User");
        }

        List<AdminPermissionDto> permDtos = new ArrayList<>();
        if (admin.getPermissions() != null) {
            for (AdminPermission p : admin.getPermissions()) {
                permDtos.add(new AdminPermissionDto(
                        p.getModuleKey(),
                        p.getModuleNameEn(),
                        p.isCanCreate(),
                        p.isCanRead(),
                        p.isCanUpdate(),
                        p.isCanDelete(),
                        p.getActions()
                ));
            }
        }
        dto.setPermissions(permDtos);

        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) {
        this.id = id;
        if (this.adminId == null) this.adminId = id;
    }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) {
        this.adminId = adminId;
        if (this.id == null) this.id = adminId;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isSuperAdmin() { return superAdmin; }
    public void setSuperAdmin(boolean superAdmin) { this.superAdmin = superAdmin; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public boolean isCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public String getPrimaryRole() { return primaryRole; }
    public void setPrimaryRole(String primaryRole) { this.primaryRole = primaryRole; }

    public List<String> getRoleNames() { return roleNames; }
    public void setRoleNames(List<String> roleNames) { this.roleNames = roleNames; }

    public List<RoleDto> getRoles() { return roles; }
    public void setRoles(List<RoleDto> roles) { this.roles = roles; }

    public List<AdminPermissionDto> getPermissions() { return permissions; }
    public void setPermissions(List<AdminPermissionDto> permissions) { this.permissions = permissions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
