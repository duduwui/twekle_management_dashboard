package com.twekl.dashboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(length = 1000)
    private String itemsSummary;

    private Double totalAmount;
    private String paymentMethod;
    private String orderStatus = "DELIVERED"; // "DELIVERED", "COMPLETED", "SHIPPED", "PROCESSING"

    @Transient
    private String followupStatus = "IDLE"; // "IDLE", "ALERT", "DONE"

    @Transient
    private Boolean isFullyFollowedUp = false;

    @Transient
    private Long ageHours = 0L;

    @Transient
    private Long ageMinutes = 0L;

    private LocalDateTime createdAt;

    public CustomerOrder() {
        this.createdAt = LocalDateTime.now();
        this.orderDate = LocalDateTime.now();
        this.orderStatus = "DELIVERED";
    }

    public CustomerOrder(Long id, Long customerId, String orderNumber, LocalDateTime orderDate, String itemsSummary, Double totalAmount, String paymentMethod, String orderStatus) {
        this.id = id;
        this.customerId = customerId;
        this.orderNumber = orderNumber;
        this.orderDate = (orderDate != null) ? orderDate : LocalDateTime.now();
        this.itemsSummary = itemsSummary;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.orderStatus = (orderStatus != null) ? orderStatus : "DELIVERED";
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (orderDate == null) orderDate = LocalDateTime.now();
        if (orderStatus == null) orderStatus = "DELIVERED";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getItemsSummary() { return itemsSummary; }
    public void setItemsSummary(String itemsSummary) { this.itemsSummary = itemsSummary; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFollowupStatus() { return followupStatus; }
    public void setFollowupStatus(String followupStatus) { this.followupStatus = followupStatus; }

    public Boolean getIsFullyFollowedUp() { return isFullyFollowedUp; }
    public void setIsFullyFollowedUp(Boolean isFullyFollowedUp) { this.isFullyFollowedUp = isFullyFollowedUp; }

    public Long getAgeHours() { return ageHours; }
    public void setAgeHours(Long ageHours) { this.ageHours = ageHours; }

    public Long getAgeMinutes() { return ageMinutes; }
    public void setAgeMinutes(Long ageMinutes) { this.ageMinutes = ageMinutes; }

    // Builder pattern
    public static CustomerOrderBuilder builder() {
        return new CustomerOrderBuilder();
    }

    public static class CustomerOrderBuilder {
        private Long id;
        private Long customerId;
        private String orderNumber;
        private LocalDateTime orderDate;
        private String itemsSummary;
        private Double totalAmount;
        private String paymentMethod;
        private String orderStatus = "DELIVERED";

        public CustomerOrderBuilder id(Long id) { this.id = id; return this; }
        public CustomerOrderBuilder customerId(Long customerId) { this.customerId = customerId; return this; }
        public CustomerOrderBuilder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public CustomerOrderBuilder orderDate(LocalDateTime orderDate) { this.orderDate = orderDate; return this; }
        public CustomerOrderBuilder itemsSummary(String itemsSummary) { this.itemsSummary = itemsSummary; return this; }
        public CustomerOrderBuilder totalAmount(Double totalAmount) { this.totalAmount = totalAmount; return this; }
        public CustomerOrderBuilder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public CustomerOrderBuilder orderStatus(String orderStatus) { this.orderStatus = orderStatus; return this; }

        public CustomerOrder build() {
            return new CustomerOrder(id, customerId, orderNumber, orderDate, itemsSummary, totalAmount, paymentMethod, orderStatus);
        }
    }
}
