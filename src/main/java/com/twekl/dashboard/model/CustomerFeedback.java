package com.twekl.dashboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_feedbacks")
public class CustomerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    private String customerName;
    private Long orderId;
    private String orderNumber;
    private String orderSummary;

    private String authorName = "Follow-up Agent";

    @Column(nullable = false)
    private String feedbackType = "COMPLIMENT"; // "COMPLIMENT", "REVIEW", "NOTE", "INQUIRY"

    @Column(length = 2000, nullable = false)
    private String content;

    private Integer rating = 5; // 1 - 5 stars
    private String status = "FOLLOWED_UP";  // "FOLLOWED_UP", "PENDING", "RESOLVED"

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(length = 30)
    private String satisfaction; // "SATISFIED", "NEUTRAL", "UNSATISFIED", or null

    private LocalDateTime createdAt;

    public CustomerFeedback() {
        this.createdAt = LocalDateTime.now();
        this.authorName = "Follow-up Agent";
        this.feedbackType = "COMPLIMENT";
        this.rating = 5;
        this.status = "FOLLOWED_UP";
    }

    public CustomerFeedback(Long id, Long customerId, String customerName, Long orderId, String orderNumber, String orderSummary, String authorName, String feedbackType, String content, Integer rating, String status, String satisfaction, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderSummary = orderSummary;
        this.authorName = (authorName != null) ? authorName : "Follow-up Agent";
        this.feedbackType = (feedbackType != null) ? feedbackType : "COMPLIMENT";
        this.content = content;
        this.rating = (rating != null) ? rating : 5;
        this.status = (status != null) ? status : "FOLLOWED_UP";
        this.satisfaction = satisfaction;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "FOLLOWED_UP";
        if (rating == null) rating = 5;
        if (authorName == null) authorName = "Follow-up Agent";
        if (feedbackType == null) feedbackType = "COMPLIMENT";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getOrderSummary() { return orderSummary; }
    public void setOrderSummary(String orderSummary) { this.orderSummary = orderSummary; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSatisfaction() { return satisfaction; }
    public void setSatisfaction(String satisfaction) { this.satisfaction = satisfaction; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static CustomerFeedbackBuilder builder() {
        return new CustomerFeedbackBuilder();
    }

    public static class CustomerFeedbackBuilder {
        private Long id;
        private Long customerId;
        private String customerName;
        private Long orderId;
        private String orderNumber;
        private String orderSummary;
        private String authorName = "Follow-up Agent";
        private String feedbackType = "COMPLIMENT";
        private String content;
        private Integer rating = 5;
        private String status = "FOLLOWED_UP";
        private String satisfaction;
        private String imageUrl;
        private LocalDateTime createdAt;

        public CustomerFeedbackBuilder id(Long id) { this.id = id; return this; }
        public CustomerFeedbackBuilder customerId(Long customerId) { this.customerId = customerId; return this; }
        public CustomerFeedbackBuilder customerName(String customerName) { this.customerName = customerName; return this; }
        public CustomerFeedbackBuilder orderId(Long orderId) { this.orderId = orderId; return this; }
        public CustomerFeedbackBuilder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public CustomerFeedbackBuilder orderSummary(String orderSummary) { this.orderSummary = orderSummary; return this; }
        public CustomerFeedbackBuilder authorName(String authorName) { this.authorName = authorName; return this; }
        public CustomerFeedbackBuilder feedbackType(String feedbackType) { this.feedbackType = feedbackType; return this; }
        public CustomerFeedbackBuilder content(String content) { this.content = content; return this; }
        public CustomerFeedbackBuilder rating(Integer rating) { this.rating = rating; return this; }
        public CustomerFeedbackBuilder status(String status) { this.status = status; return this; }
        public CustomerFeedbackBuilder satisfaction(String satisfaction) { this.satisfaction = satisfaction; return this; }
        public CustomerFeedbackBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public CustomerFeedbackBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CustomerFeedback build() {
            CustomerFeedback fb = new CustomerFeedback(id, customerId, customerName, orderId, orderNumber, orderSummary, authorName, feedbackType, content, rating, status, satisfaction, createdAt);
            fb.setImageUrl(imageUrl);
            return fb;
        }
    }
}
