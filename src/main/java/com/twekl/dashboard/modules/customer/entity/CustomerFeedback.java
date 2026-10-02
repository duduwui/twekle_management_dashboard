package com.twekl.dashboard.modules.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_feedbacks")
public class CustomerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    private Long orderId;

    @Column(nullable = false, length = 50)
    private String type; // NOTE, COMPLIMENT, REVIEW, COMPLAINT

    @Column(columnDefinition = "TEXT")
    private String content;

    private String photoUrl;
    private int rating = 5; // 1-5 stars

    private String milestoneLabel;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public CustomerFeedback() {
        this.createdAt = LocalDateTime.now();
    }

    public CustomerFeedback(Customer customer, Long orderId, String type, String content, String photoUrl, int rating, String milestoneLabel) {
        this.customer = customer;
        this.orderId = orderId;
        this.type = type;
        this.content = content;
        this.photoUrl = photoUrl;
        this.rating = rating;
        this.milestoneLabel = milestoneLabel;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getMilestoneLabel() { return milestoneLabel; }
    public void setMilestoneLabel(String milestoneLabel) { this.milestoneLabel = milestoneLabel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
