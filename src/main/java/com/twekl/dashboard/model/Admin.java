package com.twekl.dashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 128)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(length = 32)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean isSuperAdmin = false;

    @Column(nullable = false)
    private boolean canCreateRoles = true;

    @Column(nullable = false)
    private boolean canManageUsers = true;

    @Column(length = 32)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Admin() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Admin(Long id, String username, String password, String phoneNumber, boolean isSuperAdmin, boolean canCreateRoles, boolean canManageUsers, String status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.isSuperAdmin = isSuperAdmin;
        this.canCreateRoles = canCreateRoles;
        this.canManageUsers = canManageUsers;
        this.status = (status != null) ? status : "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Builder
    public static AdminBuilder builder() {
        return new AdminBuilder();
    }

    public static class AdminBuilder {
        private Long id;
        private String username;
        private String password;
        private String phoneNumber;
        private boolean isSuperAdmin = false;
        private boolean canCreateRoles = true;
        private boolean canManageUsers = true;
        private String status = "ACTIVE";

        public AdminBuilder id(Long id) { this.id = id; return this; }
        public AdminBuilder username(String username) { this.username = username; return this; }
        public AdminBuilder password(String password) { this.password = password; return this; }
        public AdminBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public AdminBuilder isSuperAdmin(boolean isSuperAdmin) { this.isSuperAdmin = isSuperAdmin; return this; }
        public AdminBuilder canCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; return this; }
        public AdminBuilder canManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; return this; }
        public AdminBuilder status(String status) { this.status = status; return this; }

        public Admin build() {
            return new Admin(id, username, password, phoneNumber, isSuperAdmin, canCreateRoles, canManageUsers, status);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
