package com.twekl.dashboard.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_followup_checks")
public class OrderFollowupCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "preset_id")
    private Long presetId;

    @Column(name = "preset_name", nullable = false)
    private String presetName;

    @Column(name = "duration_value")
    private Integer durationValue;

    @Column(name = "duration_unit")
    private String durationUnit;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "note", length = 2000)
    private String note;

    @Lob
    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OrderFollowupCheck() {
        this.isCompleted = false;
    }

    public OrderFollowupCheck(Long id, Long orderId, Long presetId, String presetName, Integer durationValue, String durationUnit, Boolean isCompleted, String note, String imageUrl, String checkedBy, LocalDateTime checkedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.presetId = presetId;
        this.presetName = presetName;
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
        this.isCompleted = isCompleted != null ? isCompleted : false;
        this.note = note;
        this.imageUrl = imageUrl;
        this.checkedBy = checkedBy;
        this.checkedAt = checkedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getPresetId() { return presetId; }
    public void setPresetId(Long presetId) { this.presetId = presetId; }

    public String getPresetName() { return presetName; }
    public void setPresetName(String presetName) { this.presetName = presetName; }

    public Integer getDurationValue() { return durationValue; }
    public void setDurationValue(Integer durationValue) { this.durationValue = durationValue; }

    public String getDurationUnit() { return durationUnit; }
    public void setDurationUnit(String durationUnit) { this.durationUnit = durationUnit; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCheckedBy() { return checkedBy; }
    public void setCheckedBy(String checkedBy) { this.checkedBy = checkedBy; }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long orderId;
        private Long presetId;
        private String presetName;
        private Integer durationValue;
        private String durationUnit;
        private Boolean isCompleted = false;
        private String note;
        private String imageUrl;
        private String checkedBy;
        private LocalDateTime checkedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder orderId(Long orderId) { this.orderId = orderId; return this; }
        public Builder presetId(Long presetId) { this.presetId = presetId; return this; }
        public Builder presetName(String presetName) { this.presetName = presetName; return this; }
        public Builder durationValue(Integer durationValue) { this.durationValue = durationValue; return this; }
        public Builder durationUnit(String durationUnit) { this.durationUnit = durationUnit; return this; }
        public Builder isCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; return this; }
        public Builder note(String note) { this.note = note; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder checkedBy(String checkedBy) { this.checkedBy = checkedBy; return this; }
        public Builder checkedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public OrderFollowupCheck build() {
            return new OrderFollowupCheck(id, orderId, presetId, presetName, durationValue, durationUnit, isCompleted, note, imageUrl, checkedBy, checkedAt, createdAt, updatedAt);
        }
    }
}
