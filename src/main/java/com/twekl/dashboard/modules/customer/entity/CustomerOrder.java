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

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber; // e.g. ORD-1001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(name = "items_summary", length = 1000)
    private String itemsSummary;

    @Column(name = "product_name")
    private String productName;

    private int quantity = 1;

    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    @Column(name = "total_price")
    private double totalPrice = 0.0;

    @Column(name = "order_status", length = 50)
    private String orderStatus = "DELIVERED"; // PENDING, DELIVERED, CANCELLED

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OrderFollowupCheck> checks = new ArrayList<>();

    public CustomerOrder() {
        this.orderDate = LocalDateTime.now();
    }

    public CustomerOrder(String orderNumber, Customer customer, String itemsSummary, int quantity, double totalAmount, LocalDateTime orderDate) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.itemsSummary = itemsSummary;
        this.productName = itemsSummary;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.totalPrice = totalAmount;
        this.orderStatus = "DELIVERED";
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

    public String getItemsSummary() {
        if (this.itemsSummary != null && !this.itemsSummary.trim().isEmpty()) {
            return this.itemsSummary;
        }
        return (this.productName != null && !this.productName.trim().isEmpty()) ? this.productName : "Standard Products";
    }

    public void setItemsSummary(String itemsSummary) {
        this.itemsSummary = itemsSummary;
        this.productName = itemsSummary;
    }

    public String getProductName() { return getItemsSummary(); }
    public void setProductName(String productName) { setItemsSummary(productName); }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Double getTotalAmount() {
        if (this.totalAmount != null && this.totalAmount > 0) return this.totalAmount;
        return this.totalPrice;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
        this.totalPrice = totalAmount != null ? totalAmount : 0.0;
    }

    public double getTotalPrice() { return getTotalAmount(); }
    public void setTotalPrice(double totalPrice) { setTotalAmount(totalPrice); }

    public String getOrderStatus() {
        return (this.orderStatus != null && !this.orderStatus.isEmpty()) ? this.orderStatus : "DELIVERED";
    }

    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getStatus() { return getOrderStatus(); }
    public void setStatus(String status) { setOrderStatus(status); }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public List<OrderFollowupCheck> getChecks() { return checks; }
    public void setChecks(List<OrderFollowupCheck> checks) { this.checks = checks; }
}
