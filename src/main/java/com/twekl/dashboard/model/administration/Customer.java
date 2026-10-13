package com.twekl.dashboard.model.administration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.twekl.dashboard.model.administration.CustomerFeedback;
import com.twekl.dashboard.model.administration.CustomerOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Pure JPA Entity representing Customers.
 */
@Entity
@Table(name = "CUSTOMERS")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Column(name = "NAME", nullable = false)
    private String name;

    @NotBlank(message = "Phone number is required")
    @Column(name = "PHONE_NUMBER", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "CITY")
    private String city;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "TOTAL_ORDERS")
    private int totalOrders = 0;

    @Column(name = "TOTAL_SPENT")
    private double totalSpent = 0.0;

    @Column(name = "LAST_ORDER_DATE")
    private Instant lastOrderDate;

    @JsonIgnore
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CustomerOrder> orders = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CustomerFeedback> feedbacks = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private Instant updatedAt;

    public Customer() {}

    public Customer(Long id, String name, String phoneNumber, String email, String city) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.city = city;
        this.active = true;
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

    public Long getCustomerId() { return id; }
    public void setCustomerId(Long customerId) { this.id = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public Instant getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(Instant lastOrderDate) { this.lastOrderDate = lastOrderDate; }

    public Set<CustomerOrder> getOrders() { return orders; }
    public void setOrders(Set<CustomerOrder> orders) { this.orders = orders; }

    public Set<CustomerFeedback> getFeedbacks() { return feedbacks; }
    public void setFeedbacks(Set<CustomerFeedback> feedbacks) { this.feedbacks = feedbacks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

