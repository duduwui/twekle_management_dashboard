package com.twekl.dashboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    private String email;
    private String city;

    private Integer totalOrders = 0;
    private Double totalSpent = 0.0;

    private LocalDateTime lastOrderDate;
    private Integer daysSinceLastOrder = 0;

    @Column(nullable = false)
    private String status = "ACTIVE"; // "FOLLOW_UP_24H", "FOLLOW_UP_7D", "DORMANT_30D", "ACTIVE"

    @Transient
    private Boolean allFollowupsCompleted = false;

    @Transient
    private String nextPendingFollowup;

    @Transient
    private Long nextPendingOrderId;

    @Transient
    private String nextPendingOrderNumber;

    @Transient
    private Integer remainingFollowupsCount = 0;

    @Transient
    private String followupStatus = "IDLE"; // "IDLE", "ALERT", "DONE"

    @Transient
    private Long hoursSinceLastOrder = 0L;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.totalOrders = 0;
        this.totalSpent = 0.0;
        this.daysSinceLastOrder = 0;
        this.status = "ACTIVE";
    }

    public Customer(Long id, String name, String phoneNumber, String email, String city, Integer totalOrders, Double totalSpent, LocalDateTime lastOrderDate, Integer daysSinceLastOrder, String status) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.city = city;
        this.totalOrders = totalOrders != null ? totalOrders : 0;
        this.totalSpent = totalSpent != null ? totalSpent : 0.0;
        this.lastOrderDate = lastOrderDate;
        this.daysSinceLastOrder = daysSinceLastOrder != null ? daysSinceLastOrder : 0;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (totalOrders == null) totalOrders = 0;
        if (totalSpent == null) totalSpent = 0.0;
        if (daysSinceLastOrder == null) daysSinceLastOrder = 0;
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public LocalDateTime getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; }

    public Integer getDaysSinceLastOrder() { return daysSinceLastOrder; }
    public void setDaysSinceLastOrder(Integer daysSinceLastOrder) { this.daysSinceLastOrder = daysSinceLastOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getAllFollowupsCompleted() { return allFollowupsCompleted; }
    public void setAllFollowupsCompleted(Boolean allFollowupsCompleted) { this.allFollowupsCompleted = allFollowupsCompleted; }

    public String getNextPendingFollowup() { return nextPendingFollowup; }
    public void setNextPendingFollowup(String nextPendingFollowup) { this.nextPendingFollowup = nextPendingFollowup; }

    public Long getNextPendingOrderId() { return nextPendingOrderId; }
    public void setNextPendingOrderId(Long nextPendingOrderId) { this.nextPendingOrderId = nextPendingOrderId; }

    public String getNextPendingOrderNumber() { return nextPendingOrderNumber; }
    public void setNextPendingOrderNumber(String nextPendingOrderNumber) { this.nextPendingOrderNumber = nextPendingOrderNumber; }

    public Integer getRemainingFollowupsCount() { return remainingFollowupsCount; }
    public void setRemainingFollowupsCount(Integer remainingFollowupsCount) { this.remainingFollowupsCount = remainingFollowupsCount; }

    public String getFollowupStatus() { return followupStatus; }
    public void setFollowupStatus(String followupStatus) { this.followupStatus = followupStatus; }

    public Long getHoursSinceLastOrder() { return hoursSinceLastOrder; }
    public void setHoursSinceLastOrder(Long hoursSinceLastOrder) { this.hoursSinceLastOrder = hoursSinceLastOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Builder pattern
    public static CustomerBuilder builder() {
        return new CustomerBuilder();
    }

    public static class CustomerBuilder {
        private Long id;
        private String name;
        private String phoneNumber;
        private String email;
        private String city;
        private Integer totalOrders = 0;
        private Double totalSpent = 0.0;
        private LocalDateTime lastOrderDate;
        private Integer daysSinceLastOrder = 0;
        private String status = "ACTIVE";

        public CustomerBuilder id(Long id) { this.id = id; return this; }
        public CustomerBuilder name(String name) { this.name = name; return this; }
        public CustomerBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public CustomerBuilder email(String email) { this.email = email; return this; }
        public CustomerBuilder city(String city) { this.city = city; return this; }
        public CustomerBuilder totalOrders(Integer totalOrders) { this.totalOrders = totalOrders; return this; }
        public CustomerBuilder totalSpent(Double totalSpent) { this.totalSpent = totalSpent; return this; }
        public CustomerBuilder lastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; return this; }
        public CustomerBuilder daysSinceLastOrder(Integer daysSinceLastOrder) { this.daysSinceLastOrder = daysSinceLastOrder; return this; }
        public CustomerBuilder status(String status) { this.status = status; return this; }

        public Customer build() {
            return new Customer(id, name, phoneNumber, email, city, totalOrders, totalSpent, lastOrderDate, daysSinceLastOrder, status);
        }
    }
}
