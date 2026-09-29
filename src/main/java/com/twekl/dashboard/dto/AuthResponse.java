package com.twekl.dashboard.dto;

public class AuthResponse {
    private boolean authenticated;
    private String username;
    private String role; // "SUPER_ADMIN", "ADMIN", "USER"
    private boolean isSuperAdmin;
    private boolean canCreateRoles;
    private boolean canManageUsers;
    private String message;

    public AuthResponse() {}

    public AuthResponse(boolean authenticated, String username, String role, boolean isSuperAdmin, boolean canCreateRoles, boolean canManageUsers, String message) {
        this.authenticated = authenticated;
        this.username = username;
        this.role = role;
        this.isSuperAdmin = isSuperAdmin;
        this.canCreateRoles = canCreateRoles;
        this.canManageUsers = canManageUsers;
        this.message = message;
    }

    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }

    public boolean isCanCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public boolean isCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
