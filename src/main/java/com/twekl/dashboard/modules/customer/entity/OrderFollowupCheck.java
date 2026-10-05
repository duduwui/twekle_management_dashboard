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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private CustomerOrder order;

    @Column(name = "preset_id")
    private Long presetId;

    @Column(name = "preset_name")
    private String presetName; // e.g. "last 24 hours", "last 7 days"

    @Column(name = "milestone_name")
    private String milestoneName;

    @Column(name = "milestone_label")
    private String milestoneLabel;

    @Column(name = "duration_value")
    private Integer durationValue = 1;

    @Column(name = "duration_unit", length = 32)
    private String durationUnit = "HOURS"; // HOURS, DAYS, MONTHS

    @Column(name = "target_days")
    private int targetDays = 1;

    @Column(name = "status", length = 20)
    private String status = "PENDING"; // PENDING, COMPLETED, OVERDUE

    @Column(name = "is_completed")
    private boolean isCompleted = false;

    @Column(name = "completed")
    private boolean completed = false;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(name = "photo_url")
    private String photoUrl;

    public OrderFollowupCheck() {}

    public OrderFollowupCheck(CustomerOrder order, String presetName, String milestoneLabel, int targetDays) {
        this.order = order;
        this.presetName = presetName;
        this.milestoneName = presetName;
        this.milestoneLabel = milestoneLabel;
        this.targetDays = targetDays;
        this.durationValue = targetDays;
        this.durationUnit = targetDays == 1 ? "HOURS" : "DAYS";
        this.status = "PENDING";
        this.isCompleted = false;
        this.completed = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public Long getPresetId() { return presetId; }
    public void setPresetId(Long presetId) { this.presetId = presetId; }

    public String getPresetName() {
        if (this.presetName != null && !this.presetName.trim().isEmpty()) {
            return this.presetName;
        }
        if (this.milestoneLabel != null && !this.milestoneLabel.trim().isEmpty()) {
            return this.milestoneLabel;
        }
        return this.milestoneName != null ? this.milestoneName : "Follow-up Milestone";
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName;
        this.milestoneName = presetName;
    }

    public String getMilestoneName() { return getPresetName(); }
    public void setMilestoneName(String milestoneName) { setPresetName(milestoneName); }

    public String getMilestoneLabel() { return getPresetName(); }
    public void setMilestoneLabel(String milestoneLabel) { this.milestoneLabel = milestoneLabel; }

    public Integer getDurationValue() { return durationValue != null ? durationValue : targetDays; }
    public void setDurationValue(Integer durationValue) { this.durationValue = durationValue; }

    public String getDurationUnit() { return durationUnit != null ? durationUnit : "DAYS"; }
    public void setDurationUnit(String durationUnit) { this.durationUnit = durationUnit; }

    public int getTargetDays() {
        if (targetDays > 0) return targetDays;
        if (durationValue != null && durationValue > 0) {
            if ("HOURS".equalsIgnoreCase(durationUnit)) return Math.max(1, durationValue / 24);
            if ("MONTHS".equalsIgnoreCase(durationUnit)) return durationValue * 30;
            return durationValue;
        }
        return 1;
    }

    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }

    public String getStatus() {
        if (getIsCompleted()) return "COMPLETED";
        if (getIsDue()) return "OVERDUE";
        return (status != null && !status.isEmpty()) ? status : "PENDING";
    }

    public void setStatus(String status) { this.status = status; }

    public boolean getIsCompleted() {
        return this.isCompleted || this.completed;
    }

    public void setIsCompleted(boolean isCompleted) {
        this.isCompleted = isCompleted;
        this.completed = isCompleted;
    }

    public boolean isCompleted() { return getIsCompleted(); }
    public void setCompleted(boolean completed) { setIsCompleted(completed); }

    public boolean getIsDue() {
        if (getIsCompleted()) return false;
        if (this.order != null && this.order.getOrderDate() != null) {
            LocalDateTime dueTime = this.order.getOrderDate();
            String unit = getDurationUnit() != null ? getDurationUnit().toUpperCase() : "HOURS";
            int val = getDurationValue() != null ? getDurationValue() : 1;
            if ("HOURS".equals(unit)) {
                dueTime = dueTime.plusHours(val);
            } else if ("WEEKS".equals(unit)) {
                dueTime = dueTime.plusWeeks(val);
            } else if ("MONTHS".equals(unit)) {
                dueTime = dueTime.plusMonths(val);
            } else {
                dueTime = dueTime.plusDays(val);
            }
            return LocalDateTime.now().isAfter(dueTime);
        }
        return false;
    }

    public Integer getMinutesUntilDue() {
        if (getIsCompleted() || this.order == null || this.order.getOrderDate() == null) return null;
        LocalDateTime dueTime = this.order.getOrderDate();
        String unit = getDurationUnit() != null ? getDurationUnit().toUpperCase() : "HOURS";
        int val = getDurationValue() != null ? getDurationValue() : 1;
        if ("HOURS".equals(unit)) {
            dueTime = dueTime.plusHours(val);
        } else if ("WEEKS".equals(unit)) {
            dueTime = dueTime.plusWeeks(val);
        } else if ("MONTHS".equals(unit)) {
            dueTime = dueTime.plusMonths(val);
        } else {
            dueTime = dueTime.plusDays(val);
        }
        long minutes = java.time.Duration.between(LocalDateTime.now(), dueTime).toMinutes();
        return (int) minutes; // positive = remaining, negative = overdue
    }


    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }

    public String getNote() {
        if (this.note != null && !this.note.trim().isEmpty()) return this.note;
        return this.notes != null ? this.notes : "";
    }

    public void setNote(String note) {
        this.note = note;
        this.notes = note;
    }

    public String getNotes() { return getNote(); }
    public void setNotes(String notes) { setNote(notes); }

    public String getImageUrl() {
        if (this.imageUrl != null && !this.imageUrl.trim().isEmpty()) return this.imageUrl;
        return this.photoUrl != null ? this.photoUrl : "";
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.photoUrl = imageUrl;
    }

    public String getPhotoUrl() { return getImageUrl(); }
    public void setPhotoUrl(String photoUrl) { setImageUrl(photoUrl); }
}
