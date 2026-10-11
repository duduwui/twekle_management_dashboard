package com.twekl.dashboard.administration.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.twekl.dashboard.administration.dto.admin.AdminPermissionDto;
import com.twekl.dashboard.administration.dto.role.RoleDto;
import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.model.admin.AdminPermission;
import com.twekl.dashboard.administration.model.role.Role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthResponse {

    private boolean authenticated;
    private String username;
    private String role;
    
    @JsonProperty("isSuperAdmin")
    @JsonAlias({"superAdmin"})
    private boolean superAdmin;

    @JsonProperty("canCreateRoles")
    private boolean canCreateRoles;

    @JsonProperty("canManageUsers")
    private boolean canManageUsers;

    @JsonProperty("canCreate")
    private boolean canCreate;

    @JsonProperty("canRead")
    private boolean canRead;

    @JsonProperty("canUpdate")
    private boolean canUpdate;

    @JsonProperty("canDelete")
    private boolean canDelete;

    private boolean canReadReports;
    private boolean canReadFollowups;
    private boolean canUpdateFollowups;
    private boolean canCreateFollowups;
    private boolean canDeleteFollowups;

    private List<AdminPermissionDto> permissions = new ArrayList<>();
    private Set<RoleDto> roles = new HashSet<>();
    private String message;

    public AuthResponse() {}

    public AuthResponse(boolean authenticated, String username, String role, boolean isSuperAdmin, String message) {
        this.authenticated = authenticated;
        this.username = username;
        this.role = role;
        this.superAdmin = isSuperAdmin;
        this.message = message;
    }

    public static AuthResponse success(String username, String role, boolean isSuperAdmin) {
        AuthResponse res = new AuthResponse(true, username, role, isSuperAdmin, "Authentication successful");
        res.setCanCreateRoles(isSuperAdmin);
        res.setCanManageUsers(isSuperAdmin);
        res.setCanCreate(isSuperAdmin);
        res.setCanRead(true);
        res.setCanUpdate(isSuperAdmin);
        res.setCanDelete(isSuperAdmin);
        res.setCanReadReports(isSuperAdmin);
        res.setCanReadFollowups(true);
        res.setCanUpdateFollowups(isSuperAdmin);
        res.setCanCreateFollowups(isSuperAdmin);
        res.setCanDeleteFollowups(isSuperAdmin);
        return res;
    }

    public static AuthResponse fromAdmin(Admin admin) {
        AuthResponse res = new AuthResponse();
        res.setAuthenticated(true);
        res.setUsername(admin.getUsername());
        res.setMessage("Authentication successful");

        boolean isSuper = admin.isSuperAdmin();
        res.setSuperAdmin(isSuper);
        res.setCanCreateRoles(isSuper || admin.isCanCreateRoles());
        res.setCanManageUsers(isSuper || admin.isCanManageUsers());

        Set<Role> adminRoles = admin.getRoles() != null ? admin.getRoles() : new HashSet<>();
        Set<RoleDto> roleDtos = new HashSet<>();
        for (Role r : adminRoles) {
            roleDtos.add(new RoleDto(r.getId(), r.getName(), r.isCanCreate(), r.isCanRead(), r.isCanUpdate(), r.isCanDelete()));
        }
        res.setRoles(roleDtos);

        if (isSuper) {
            res.setRole("Super Admin");
            res.setCanCreate(true);
            res.setCanRead(true);
            res.setCanUpdate(true);
            res.setCanDelete(true);
        } else if (!adminRoles.isEmpty()) {
            String roleNames = adminRoles.stream().map(Role::getName).collect(Collectors.joining(", "));
            res.setRole(roleNames);
            res.setCanCreate(adminRoles.stream().anyMatch(Role::isCanCreate));
            res.setCanRead(adminRoles.stream().anyMatch(Role::isCanRead));
            res.setCanUpdate(adminRoles.stream().anyMatch(Role::isCanUpdate));
            res.setCanDelete(adminRoles.stream().anyMatch(Role::isCanDelete));
        } else {
            res.setRole("Administrator");
            res.setCanCreate(false);
            res.setCanRead(true);
            res.setCanUpdate(false);
            res.setCanDelete(false);
        }

        // Module permissions resolution
        List<AdminPermissionDto> permDtos = new ArrayList<>();
        boolean canReadReports = isSuper;
        boolean canReadFollowups = isSuper;
        boolean canUpdateFollowups = isSuper;
        boolean canCreateFollowups = isSuper;
        boolean canDeleteFollowups = isSuper;

        if (admin.getPermissions() != null && !admin.getPermissions().isEmpty()) {
            for (AdminPermission p : admin.getPermissions()) {
                permDtos.add(new AdminPermissionDto(p.getModuleKey(), p.getModuleNameEn(),
                        p.isCanCreate(), p.isCanRead(), p.isCanUpdate(), p.isCanDelete()));
                if ("REPORTS_INSIGHTS".equalsIgnoreCase(p.getModuleKey())) {
                    canReadReports = isSuper || (p.isCanRead() || p.isCanCreate() || p.isCanUpdate() || p.isCanDelete());
                } else if ("CUSTOMER_FOLLOWUP".equalsIgnoreCase(p.getModuleKey())) {
                    canReadFollowups = isSuper || (p.isCanRead() || p.isCanCreate() || p.isCanUpdate() || p.isCanDelete());
                    canUpdateFollowups = isSuper || p.isCanUpdate();
                    canCreateFollowups = isSuper || p.isCanCreate();
                    canDeleteFollowups = isSuper || p.isCanDelete();
                } else if ("ADMIN_MANAGEMENT".equalsIgnoreCase(p.getModuleKey())) {
                    if (!isSuper) {
                        res.setCanCreate(p.isCanCreate());
                        res.setCanRead(p.isCanRead());
                        res.setCanUpdate(p.isCanUpdate());
                        res.setCanDelete(p.isCanDelete());
                        boolean anyAdmin = p.isCanRead() || p.isCanCreate() || p.isCanUpdate() || p.isCanDelete();
                        res.setCanManageUsers(anyAdmin && admin.isCanManageUsers());
                    }
                }
            }
        } else {
            if (isSuper) {
                canReadReports = true;
                canReadFollowups = true;
                canUpdateFollowups = true;
                canCreateFollowups = true;
                canDeleteFollowups = true;
            } else if (!adminRoles.isEmpty()) {
                boolean rRead = adminRoles.stream().anyMatch(Role::isCanRead);
                boolean rUpdate = adminRoles.stream().anyMatch(Role::isCanUpdate);
                boolean rCreate = adminRoles.stream().anyMatch(Role::isCanCreate);
                boolean rDelete = adminRoles.stream().anyMatch(Role::isCanDelete);
                canReadReports = rRead;
                canReadFollowups = rRead;
                canUpdateFollowups = rUpdate;
                canCreateFollowups = rCreate;
                canDeleteFollowups = rDelete;
            }
        }

        res.setPermissions(permDtos);
        res.setCanReadReports(canReadReports);
        res.setCanReadFollowups(canReadFollowups);
        res.setCanUpdateFollowups(canUpdateFollowups);
        res.setCanCreateFollowups(canCreateFollowups);
        res.setCanDeleteFollowups(canDeleteFollowups);

        return res;
    }

    public static AuthResponse failed(String message) {
        return new AuthResponse(false, null, null, false, message);
    }

    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isSuperAdmin() { return superAdmin; }
    public void setIsSuperAdmin(boolean isSuperAdmin) { this.superAdmin = isSuperAdmin; }

    public boolean getSuperAdmin() { return superAdmin; }
    public void setSuperAdmin(boolean superAdmin) { this.superAdmin = superAdmin; }

    public boolean isCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public boolean isCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public boolean isCanCreate() { return canCreate; }
    public void setCanCreate(boolean canCreate) { this.canCreate = canCreate; }

    public boolean isCanRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }

    public boolean isCanUpdate() { return canUpdate; }
    public void setCanUpdate(boolean canUpdate) { this.canUpdate = canUpdate; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    public boolean isCanReadReports() { return canReadReports; }
    public void setCanReadReports(boolean canReadReports) { this.canReadReports = canReadReports; }

    public boolean isCanReadFollowups() { return canReadFollowups; }
    public void setCanReadFollowups(boolean canReadFollowups) { this.canReadFollowups = canReadFollowups; }

    public boolean isCanUpdateFollowups() { return canUpdateFollowups; }
    public void setCanUpdateFollowups(boolean canUpdateFollowups) { this.canUpdateFollowups = canUpdateFollowups; }

    public boolean isCanCreateFollowups() { return canCreateFollowups; }
    public void setCanCreateFollowups(boolean canCreateFollowups) { this.canCreateFollowups = canCreateFollowups; }

    public boolean isCanDeleteFollowups() { return canDeleteFollowups; }
    public void setCanDeleteFollowups(boolean canDeleteFollowups) { this.canDeleteFollowups = canDeleteFollowups; }

    public List<AdminPermissionDto> getPermissions() { return permissions; }
    public void setPermissions(List<AdminPermissionDto> permissions) { this.permissions = permissions; }

    public Set<RoleDto> getRoles() { return roles; }
    public void setRoles(Set<RoleDto> roles) { this.roles = roles; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

