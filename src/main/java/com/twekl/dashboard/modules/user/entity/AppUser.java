package com.twekl.dashboard.modules.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "English username is required")
    @Column(nullable = false, unique = true, length = 100)
    private String usernameEn;

    @Column(length = 100)
    private String usernameAr;

    @Column(length = 100)
    private String usernameKu;

    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 128)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(length = 50)
    private String phoneNumber;

    @Column(length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ModulePermission> permissions = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public AppUser() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public AppUser(Long id, String usernameEn, String usernameAr, String usernameKu, String password, String phoneNumber, String status) {
        this.id = id;
        this.usernameEn = usernameEn;
        this.usernameAr = usernameAr;
        this.usernameKu = usernameKu;
        this.password = password;
        this.phoneNumber = phoneNumber;
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

    public void addPermission(ModulePermission permission) {
        permissions.add(permission);
        permission.setUser(this);
    }

    public void removePermission(ModulePermission permission) {
        permissions.remove(permission);
        permission.setUser(null);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsernameEn() { return usernameEn; }
    public void setUsernameEn(String usernameEn) { this.usernameEn = usernameEn; }

    public String getUsernameAr() { return usernameAr; }
    public void setUsernameAr(String usernameAr) { this.usernameAr = usernameAr; }

    public String getUsernameKu() { return usernameKu; }
    public void setUsernameKu(String usernameKu) { this.usernameKu = usernameKu; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<ModulePermission> getPermissions() { return permissions; }
    public void setPermissions(List<ModulePermission> permissions) { this.permissions = permissions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
