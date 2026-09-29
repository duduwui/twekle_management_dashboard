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

    private String authorName = "Follow-up Agent";

    @Column(nullable = false)
    private String feedbackType = "COMPLIMENT"; // "COMPLIMENT", "REVIEW", "NOTE", "INQUIRY"

    @Column(length = 2000, nullable = false)
    private String content;

    private Integer rating = 5; // 1 - 5 stars
    private String status = "FOLLOWED_UP";  // "FOLLOWED_UP", "PENDING", "RESOLVED"

    private LocalDateTime createdAt;

    public CustomerFeedback() {
        this.createdAt = LocalDateTime.now();
        this.authorName = "Follow-up Agent";
        this.feedbackType = "COMPLIMENT";
        this.rating = 5;
        this.status = "FOLLOWED_UP";
    }

    public CustomerFeedback(Long id, Long customerId, String authorName, String feedbackType, String content, Integer rating, String status, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.authorName = (authorName != null) ? authorName : "Follow-up Agent";
        this.feedbackType = (feedbackType != null) ? feedbackType : "COMPLIMENT";
        this.content = content;
        this.rating = (rating != null) ? rating : 5;
        this.status = (status != null) ? status : "FOLLOWED_UP";
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static CustomerFeedbackBuilder builder() {
        return new CustomerFeedbackBuilder();
    }

    public static class CustomerFeedbackBuilder {
        private Long id;
        private Long customerId;
        private String authorName = "Follow-up Agent";
        private String feedbackType = "COMPLIMENT";
        private String content;
        private Integer rating = 5;
        private String status = "FOLLOWED_UP";
        private LocalDateTime createdAt;

        public CustomerFeedbackBuilder id(Long id) { this.id = id; return this; }
        public CustomerFeedbackBuilder customerId(Long customerId) { this.customerId = customerId; return this; }
        public CustomerFeedbackBuilder authorName(String authorName) { this.authorName = authorName; return this; }
        public CustomerFeedbackBuilder feedbackType(String feedbackType) { this.feedbackType = feedbackType; return this; }
        public CustomerFeedbackBuilder content(String content) { this.content = content; return this; }
        public CustomerFeedbackBuilder rating(Integer rating) { this.rating = rating; return this; }
        public CustomerFeedbackBuilder status(String status) { this.status = status; return this; }
        public CustomerFeedbackBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CustomerFeedback build() {
            return new CustomerFeedback(id, customerId, authorName, feedbackType, content, rating, status, createdAt);
        }
    }
}
