package com.twekl.dashboard.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "time_filter_presets")
public class TimeFilterPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "duration_value", nullable = false)
    private Integer durationValue; // e.g. 20, 24, 7, 2, 30

    @Column(name = "duration_unit", nullable = false, length = 20)
    private String durationUnit; // HOURS, DAYS, WEEKS, MONTHS

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public TimeFilterPreset() {
    }

    public TimeFilterPreset(Long id, String name, Integer durationValue, String durationUnit, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDurationValue() {
        return durationValue;
    }

    public void setDurationValue(Integer durationValue) {
        this.durationValue = durationValue;
    }

    public String getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(String durationUnit) {
        this.durationUnit = durationUnit;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private Integer durationValue;
        private String durationUnit = "HOURS";
        private Boolean isActive = true;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder durationValue(Integer durationValue) {
            this.durationValue = durationValue;
            return this;
        }

        public Builder durationUnit(String durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TimeFilterPreset build() {
            return new TimeFilterPreset(id, name, durationValue, durationUnit, isActive, createdAt);
        }
    }

    /**
     * Helper to compute total hours for filtering calculations.
     */
    public long toTotalHours() {
        if (durationValue == null) return 24L;
        String unit = durationUnit != null ? durationUnit.toUpperCase() : "HOURS";
        return switch (unit) {
            case "HOURS" -> durationValue;
            case "DAYS" -> durationValue * 24L;
            case "WEEKS" -> durationValue * 24L * 7L;
            case "MONTHS" -> durationValue * 24L * 30L;
            default -> durationValue;
        };
    }
}
