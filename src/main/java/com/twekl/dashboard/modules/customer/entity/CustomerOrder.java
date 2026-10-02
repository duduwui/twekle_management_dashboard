package com.twekl.dashboard.modules.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderNumber; // e.g. ORD-1001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(nullable = false)
    private String productName;

    private int quantity = 1;
    private double totalPrice = 0.0;
    private String status = "DELIVERED"; // PENDING, DELIVERED, CANCELLED

    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderFollowupCheck> checks = new ArrayList<>();

    public CustomerOrder() {
        this.orderDate = LocalDateTime.now();
    }

    public CustomerOrder(String orderNumber, Customer customer, String productName, int quantity, double totalPrice, LocalDateTime orderDate) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.productName = productName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.orderDate = orderDate != null ? orderDate : LocalDateTime.now();
    }

    public void addCheck(OrderFollowupCheck check) {
        checks.add(check);
        check.setOrder(this);
    }

    public void removeCheck(OrderFollowupCheck check) {
        checks.remove(check);
        check.setOrder(null);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public List<OrderFollowupCheck> getChecks() { return checks; }
    public void setChecks(List<OrderFollowupCheck> checks) { this.checks = checks; }
}
