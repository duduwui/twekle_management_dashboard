package com.twekl.dashboard.modules.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, length = 50)
    private String phoneNumber;

    private String email;
    private String city;

    private int totalOrders = 0;
    private double totalSpent = 0.0;
    private LocalDateTime lastOrderDate;

    @Transient
    private String followupStatus = "IDLE"; // ALERT, IDLE, DONE

    @Transient
    private String nextPendingFollowup;

    @Transient
    private Long nextPendingOrderId;

    @Transient
    private boolean allFollowupsCompleted = false;

    @Transient
    private int remainingFollowupsCount = 0;

    /** Minutes until the next pending followup is due. Negative = already overdue. */
    @Transient
    private Integer nextPendingMinutesLeft = null;



    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CustomerOrder> orders = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CustomerFeedback> feedbacks = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Customer() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Customer(Long id, String name, String phoneNumber, String email, String city) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.city = city;
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

    public void addOrder(CustomerOrder order) {
        orders.add(order);
        order.setCustomer(this);
    }

    public void removeOrder(CustomerOrder order) {
        orders.remove(order);
        order.setCustomer(null);
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

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public LocalDateTime getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; }

    public String getFollowupStatus() { return followupStatus; }
    public void setFollowupStatus(String followupStatus) { this.followupStatus = followupStatus; }

    public String getNextPendingFollowup() { return nextPendingFollowup; }
    public void setNextPendingFollowup(String nextPendingFollowup) { this.nextPendingFollowup = nextPendingFollowup; }

    public Long getNextPendingOrderId() { return nextPendingOrderId; }
    public void setNextPendingOrderId(Long nextPendingOrderId) { this.nextPendingOrderId = nextPendingOrderId; }

    public boolean isAllFollowupsCompleted() { return allFollowupsCompleted; }
    public boolean getAllFollowupsCompleted() { return allFollowupsCompleted; }
    public void setAllFollowupsCompleted(boolean allFollowupsCompleted) { this.allFollowupsCompleted = allFollowupsCompleted; }

    public int getRemainingFollowupsCount() { return remainingFollowupsCount; }
    public void setRemainingFollowupsCount(int remainingFollowupsCount) { this.remainingFollowupsCount = remainingFollowupsCount; }

    public Integer getNextPendingMinutesLeft() { return nextPendingMinutesLeft; }
    public void setNextPendingMinutesLeft(Integer nextPendingMinutesLeft) { this.nextPendingMinutesLeft = nextPendingMinutesLeft; }



    public List<CustomerOrder> getOrders() { return orders; }
    public void setOrders(List<CustomerOrder> orders) { this.orders = orders; }

    public List<CustomerFeedback> getFeedbacks() { return feedbacks; }
    public void setFeedbacks(List<CustomerFeedback> feedbacks) { this.feedbacks = feedbacks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
