package com.twekl.dashboard.modules.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_followup_checks")
public class OrderFollowupCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private CustomerOrder order;

    @Column(nullable = false, length = 100)
    private String milestoneName; // e.g. "24_HOURS", "1_WEEK", "1_MONTH"

    @Column(nullable = false, length = 100)
    private String milestoneLabel; // e.g. "24 Hours Ago"

    private int targetDays = 1;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, COMPLETED, OVERDUE

    private boolean completed = false;

    private LocalDateTime completedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String photoUrl;

    public OrderFollowupCheck() {}

    public OrderFollowupCheck(CustomerOrder order, String milestoneName, String milestoneLabel, int targetDays) {
        this.order = order;
        this.milestoneName = milestoneName;
        this.milestoneLabel = milestoneLabel;
        this.targetDays = targetDays;
        this.status = "PENDING";
        this.completed = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }

    public String getMilestoneLabel() { return milestoneLabel; }
    public void setMilestoneLabel(String milestoneLabel) { this.milestoneLabel = milestoneLabel; }

    public int getTargetDays() { return targetDays; }
    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
