package com.twekl.dashboard.model.administration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.twekl.dashboard.model.administration.Customer;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Pure JPA Entity representing Customer Orders.
 * Isolated in order domain with explicit domain identifier bridges.
 */
@Entity
@Table(name = "CUSTOMER_ORDERS")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ORDER_NUMBER", nullable = false, unique = true, length = 32)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private Customer customer;

    @Column(name = "ITEMS_SUMMARY", length = 500)
    private String itemsSummary;

    @Column(name = "QUANTITY")
    private int quantity = 1;

    @Column(name = "TOTAL_AMOUNT")
    private Double totalAmount = 0.0;

    @Column(name = "ORDER_STATUS", length = 20)
    private String orderStatus = "DELIVERED"; // PENDING, DELIVERED, CANCELLED

    @CreationTimestamp
    @Column(name = "ORDER_DATE", nullable = false)
    private Instant orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<OrderFollowupCheck> checks = new LinkedHashSet<>();

    public CustomerOrder() {}

    public CustomerOrder(String orderNumber, Customer customer, String itemsSummary, int quantity, double totalAmount, Instant orderDate) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.itemsSummary = itemsSummary;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.orderStatus = "DELIVERED";
        this.orderDate = orderDate != null ? orderDate : Instant.now();
    }

    public void addCheck(OrderFollowupCheck check) {
        checks.add(check);
        check.setOrder(this);
    }

    public void removeCheck(OrderFollowupCheck check) {
        checks.remove(check);
        check.setOrder(null);
    }

    // Standard JPA ID getters & explicit domain orderId bridges
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return id; }
    public void setOrderId(Long orderId) { this.id = orderId; }

    public Long getCustomerId() {
        return (customer != null) ? customer.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("customerId")
    public void setCustomerId(Long customerId) {
        if (customerId != null) {
            if (this.customer == null) {
                this.customer = new Customer();
            }
            this.customer.setId(customerId);
        }
    }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getItemsSummary() { return itemsSummary; }
    public void setItemsSummary(String itemsSummary) { this.itemsSummary = itemsSummary; }

    @com.fasterxml.jackson.annotation.JsonProperty("productSummary")
    public void setProductSummary(String productSummary) {
        this.itemsSummary = productSummary;
    }

    @JsonIgnore
    public String getProductName() { return itemsSummary; }
    public void setProductName(String productName) { this.itemsSummary = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Double getTotalAmount() { return totalAmount != null ? totalAmount : 0.0; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    @JsonIgnore
    public double getTotalPrice() { return getTotalAmount(); }
    public void setTotalPrice(double totalPrice) { this.totalAmount = totalPrice; }

    public String getOrderStatus() { return orderStatus != null ? orderStatus : "DELIVERED"; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    @JsonIgnore
    public String getStatus() { return getOrderStatus(); }
    public void setStatus(String status) { setOrderStatus(status); }

    public Instant getOrderDate() { return orderDate; }
    public void setOrderDate(Instant orderDate) { this.orderDate = orderDate; }

    public Set<OrderFollowupCheck> getChecks() { return checks; }
    public void setChecks(Set<OrderFollowupCheck> checks) { this.checks = checks; }
}
