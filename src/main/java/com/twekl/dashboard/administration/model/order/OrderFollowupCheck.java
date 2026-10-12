package com.twekl.dashboard.administration.model.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.twekl.dashboard.administration.model.timefilter.TimeFilterPreset;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Pure JPA Entity representing Follow-up milestones for customer orders.
 * Isolated in order domain with explicit checkId and orderId bridges.
 */
@Entity
@Table(name = "ORDER_FOLLOWUP_CHECKS")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderFollowupCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    @JsonIgnore
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRESET_ID")
    @JsonIgnore
    private TimeFilterPreset preset;

    @Column(name = "MILESTONE_NAME")
    private String milestoneName;

    @Column(name = "DURATION_VALUE")
    private Integer durationValue = 1;

    @Column(name = "DURATION_UNIT", length = 10)
    private String durationUnit = "HOURS"; // HOURS, DAYS, WEEKS, MONTHS

    @Column(name = "TARGET_DAYS")
    private int targetDays = 1;

    @Column(name = "STATUS", length = 15)
    private String status = "PENDING"; // PENDING, COMPLETED, OVERDUE

    @Column(name = "COMPLETED", nullable = false)
    private boolean completed = false;

    @Column(name = "COMPLETED_DATE")
    private Instant completedDate;

    @Column(name = "NOTES", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "IMAGE_URL", columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(name = "CHECKED_BY", length = 128)
    private String checkedBy;

    @Column(name = "CHECKED_AT")
    private Instant checkedAt;

    @Column(name = "SATISFACTION", length = 15)
    private String satisfaction;

    @Column(name = "CREATED_AT")
    private Instant createdAt;

    @Column(name = "UPDATED_AT")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public OrderFollowupCheck() {}

    public OrderFollowupCheck(CustomerOrder order, String milestoneName, int targetDays) {
        this.order = order;
        this.milestoneName = milestoneName;
        this.targetDays = targetDays;
        this.durationValue = targetDays;
        this.durationUnit = "DAYS";
        this.status = "PENDING";
        this.completed = false;
    }

    public OrderFollowupCheck(CustomerOrder order, String presetName, String milestoneLabel, int targetDays) {
        this.order = order;
        this.milestoneName = (milestoneLabel != null && !milestoneLabel.trim().isEmpty()) ? milestoneLabel : presetName;
        this.targetDays = targetDays;
        this.durationValue = targetDays;
        this.durationUnit = "DAYS";
        this.status = "PENDING";
        this.completed = false;
    }

    public OrderFollowupCheck(CustomerOrder order, TimeFilterPreset preset) {
        this.order = order;
        this.preset = preset;
        if (preset != null) {
            this.milestoneName = preset.getName();
            this.durationValue = preset.getDurationValue();
            this.durationUnit = preset.getDurationUnit();
            this.targetDays = preset.getTargetDays();
        }
        this.status = "PENDING";
        this.completed = false;
    }

    public int getEquivalentDurationMinutes() {
        String u = durationUnit != null ? durationUnit.toUpperCase() : "DAYS";
        int v = durationValue != null && durationValue > 0 ? durationValue : (targetDays > 0 ? targetDays : 1);
        return switch (u) {
            case "HOURS" -> v * 60;
            case "WEEKS" -> v * 7 * 1440;
            case "MONTHS" -> v * 30 * 1440;
            default -> v * 1440;
        };
    }

    // Standard JPA ID getters & explicit domain checkId bridges
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCheckId() { return id; }
    public void setCheckId(Long checkId) { this.id = checkId; }

    public Long getOrderId() {
        return (order != null) ? order.getId() : null;
    }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public TimeFilterPreset getPreset() { return preset; }
    public void setPreset(TimeFilterPreset preset) { this.preset = preset; }

    public Long getPresetId() {
        return (preset != null) ? preset.getId() : null;
    }

    public void setPresetId(Long presetId) {
        // Handled via setPreset when resolving entities
    }

    public String getMilestoneName() {
        if (milestoneName != null && !milestoneName.trim().isEmpty()) {
            return milestoneName;
        }
        return "Follow-up Milestone";
    }

    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }

    @com.fasterxml.jackson.annotation.JsonProperty("presetName")
    public String getPresetName() { return getMilestoneName(); }
    public void setPresetName(String presetName) { setMilestoneName(presetName); }

    @com.fasterxml.jackson.annotation.JsonProperty("milestoneLabel")
    public String getMilestoneLabel() { return getMilestoneName(); }
    public void setMilestoneLabel(String milestoneLabel) { setMilestoneName(milestoneLabel); }

    public Integer getDurationValue() { return durationValue != null ? durationValue : targetDays; }
    public void setDurationValue(Integer durationValue) { this.durationValue = durationValue; }

    public String getDurationUnit() { return durationUnit != null ? durationUnit : "DAYS"; }
    public void setDurationUnit(String durationUnit) { this.durationUnit = durationUnit; }

    public int getTargetDays() {
        if (targetDays > 0) return targetDays;
        if (durationValue != null && durationValue > 0) {
            if ("HOURS".equalsIgnoreCase(durationUnit)) return Math.max(1, durationValue / 24);
            if ("WEEKS".equalsIgnoreCase(durationUnit)) return durationValue * 7;
            if ("MONTHS".equalsIgnoreCase(durationUnit)) return durationValue * 30;
            return durationValue;
        }
        return 1;
    }

    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }

    public String getStatus() { return (status != null && !status.isEmpty()) ? status : "PENDING"; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    @com.fasterxml.jackson.annotation.JsonProperty("isCompleted")
    public boolean getIsCompleted() { return isCompleted(); }
    public void setIsCompleted(boolean isCompleted) { setCompleted(isCompleted); }

    public Instant getCompletedDate() { return completedDate; }
    public void setCompletedDate(Instant completedDate) { this.completedDate = completedDate; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    @com.fasterxml.jackson.annotation.JsonProperty("note")
    public String getNote() { return getNotes(); }
    public void setNote(String note) { setNotes(note); }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @com.fasterxml.jackson.annotation.JsonProperty("photoUrl")
    public String getPhotoUrl() { return getImageUrl(); }
    public void setPhotoUrl(String photoUrl) { setImageUrl(photoUrl); }

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("isDue")
    private Boolean isDue;

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("minutesUntilDue")
    private Integer minutesUntilDue;

    public Boolean getIsDue() { return isDue; }
    public void setIsDue(Boolean isDue) { this.isDue = isDue; }

    public Integer getMinutesUntilDue() { return minutesUntilDue; }
    public void setMinutesUntilDue(Integer minutesUntilDue) { this.minutesUntilDue = minutesUntilDue; }

    public String getCheckedBy() { return checkedBy; }
    public void setCheckedBy(String checkedBy) { this.checkedBy = checkedBy; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }

    public String getSatisfaction() { return satisfaction; }
    public void setSatisfaction(String satisfaction) { this.satisfaction = satisfaction; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
