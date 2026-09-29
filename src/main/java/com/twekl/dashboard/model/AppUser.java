package com.twekl.dashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "English username is required")
    @Column(nullable = false, unique = true, length = 64)
    private String usernameEn;

    @Column(length = 64)
    private String usernameAr;

    @Column(length = 64)
    private String usernameKu;

    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 128)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(length = 32)
    private String phoneNumber;

    @Column(length = 32)
    private String status = "ACTIVE";

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

    public static AppUserBuilder builder() {
        return new AppUserBuilder();
    }

    public static class AppUserBuilder {
        private Long id;
        private String usernameEn;
        private String usernameAr;
        private String usernameKu;
        private String password;
        private String phoneNumber;
        private String status = "ACTIVE";

        public AppUserBuilder id(Long id) { this.id = id; return this; }
        public AppUserBuilder usernameEn(String usernameEn) { this.usernameEn = usernameEn; return this; }
        public AppUserBuilder usernameAr(String usernameAr) { this.usernameAr = usernameAr; return this; }
        public AppUserBuilder usernameKu(String usernameKu) { this.usernameKu = usernameKu; return this; }
        public AppUserBuilder password(String password) { this.password = password; return this; }
        public AppUserBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public AppUserBuilder status(String status) { this.status = status; return this; }

        public AppUser build() {
            return new AppUser(id, usernameEn, usernameAr, usernameKu, password, phoneNumber, status);
        }
    }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
