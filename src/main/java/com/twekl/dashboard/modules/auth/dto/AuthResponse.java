package com.twekl.dashboard.modules.auth.dto;

public class AuthResponse {

    private String username;
    private String userType; // SUPER_ADMIN, ADMIN, USER
    private boolean isSuperAdmin;
    private boolean canCreateRoles;
    private boolean canManageUsers;

    public AuthResponse() {}

    public AuthResponse(String username, String userType, boolean isSuperAdmin, boolean canCreateRoles, boolean canManageUsers) {
        this.username = username;
        this.userType = userType;
        this.isSuperAdmin = isSuperAdmin;
        this.canCreateRoles = canCreateRoles;
        this.canManageUsers = canManageUsers;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }

    public boolean isCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public boolean isCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; }
}
