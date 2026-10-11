package com.twekl.dashboard.administration.model.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.twekl.dashboard.administration.model.role.Role;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Pure JPA Entity representing Administrators and Staff.
 * Unified system user with role and permission assignments.
 */
@Entity
@Table(name = "ADMINS")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(name = "USERNAME", nullable = false, unique = true, length = 64)
    private String username;

    @NotBlank(message = "Password is required")
    @Column(name = "PASSWORD", nullable = false, length = 128)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "PHONE_NUMBER", length = 20)
    private String phoneNumber;

    @Column(name = "IS_SUPER_ADMIN", nullable = false)
    private boolean isSuperAdmin = false;

    @Column(name = "CAN_CREATE_ROLES", nullable = false)
    private boolean canCreateRoles = true;

    @Column(name = "CAN_MANAGE_USERS", nullable = false)
    private boolean canManageUsers = true;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "ADMIN_ROLES",
        joinColumns = @JoinColumn(name = "ADMIN_ID"),
        inverseJoinColumns = @JoinColumn(name = "ROLE_ID")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<AdminPermission> permissions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private Instant updatedAt;

    public Admin() {}

    public Admin(Long id, String username, String password, String phoneNumber, boolean isSuperAdmin, boolean canCreateRoles, boolean canManageUsers, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.isSuperAdmin = isSuperAdmin;
        this.canCreateRoles = canCreateRoles;
        this.canManageUsers = canManageUsers;
        this.active = active;
    }

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
        private boolean active = true;

        public AdminBuilder id(Long id) { this.id = id; return this; }
        public AdminBuilder username(String username) { this.username = username; return this; }
        public AdminBuilder password(String password) { this.password = password; return this; }
        public AdminBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public AdminBuilder isSuperAdmin(boolean isSuperAdmin) { this.isSuperAdmin = isSuperAdmin; return this; }
        public AdminBuilder canCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; return this; }
        public AdminBuilder canManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; return this; }
        public AdminBuilder active(boolean active) { this.active = active; return this; }

        public Admin build() {
            return new Admin(id, username, password, phoneNumber, isSuperAdmin, canCreateRoles, canManageUsers, active);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAdminId() { return id; }
    public void setAdminId(Long adminId) { this.id = adminId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean isSuperAdmin) { this.isSuperAdmin = isSuperAdmin; }

    public boolean isCanCreateRoles() { return canCreateRoles; }
    public boolean canCreateRoles() { return canCreateRoles; }
    public void setCanCreateRoles(boolean canCreateRoles) { this.canCreateRoles = canCreateRoles; }

    public boolean isCanManageUsers() { return canManageUsers; }
    public boolean canManageUsers() { return canManageUsers; }
    public void setCanManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Set<AdminPermission> getPermissions() { return permissions; }
    public void setPermissions(Set<AdminPermission> permissions) {
        this.permissions.clear();
        if (permissions != null) {
            for (AdminPermission p : permissions) {
                p.setAdmin(this);
                this.permissions.add(p);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Admin admin = (Admin) o;
        return Objects.equals(username, admin.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}

