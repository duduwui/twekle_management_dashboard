package com.twekl.dashboard.model.administration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.twekl.dashboard.model.administration.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Pure JPA Entity representing Customer Feedback.
 * Isolated in order domain with explicit feedbackId, customerId, and orderId bridges.
 */
@Entity
@Table(name = "CUSTOMER_FEEDBACKS")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CustomerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    @JsonIgnore
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    @JsonIgnore
    private CustomerOrder order;

    @Column(name = "RATING", nullable = false)
    private int rating = 5;

    @Column(name = "COMMENT", length = 1000)
    private String comment;

    @Column(name = "FEEDBACK_DATE", nullable = false)
    private Instant feedbackDate;

    @PrePersist
    protected void onCreate() {
        if (this.feedbackDate == null) {
            this.feedbackDate = Instant.now();
        }
    }

    public CustomerFeedback() {}

    public CustomerFeedback(Customer customer, CustomerOrder order, int rating, String comment) {
        this.customer = customer;
        this.order = order;
        this.rating = rating;
        this.comment = comment;
        this.feedbackDate = Instant.now();
    }

    // Standard JPA ID getters & explicit domain feedbackId bridges
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFeedbackId() { return id; }
    public void setFeedbackId(Long feedbackId) { this.id = feedbackId; }

    public Long getCustomerId() {
        return (customer != null) ? customer.getId() : null;
    }

    public Long getOrderId() {
        return (order != null) ? order.getId() : null;
    }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getFeedbackDate() { return feedbackDate; }
    public void setFeedbackDate(Instant feedbackDate) { this.feedbackDate = feedbackDate; }
}
