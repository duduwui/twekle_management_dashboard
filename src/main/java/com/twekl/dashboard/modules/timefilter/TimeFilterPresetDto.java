package com.twekl.dashboard.modules.timefilter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class TimeFilterPresetDto {

    private Long id;

    @NotBlank(message = "Preset name is required")
    @Size(max = 100, message = "Preset name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Duration value is required")
    @Min(value = 1, message = "Duration value must be at least 1")
    private Integer durationValue;

    @NotBlank(message = "Duration unit is required")
    private String durationUnit;

    private Boolean isActive = true;

    private LocalDateTime createdAt;

    public TimeFilterPresetDto() {}

    public TimeFilterPresetDto(Long id, String name, Integer durationValue, String durationUnit, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private Integer durationValue;
        private String durationUnit;
        private Boolean isActive = true;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder durationValue(Integer durationValue) { this.durationValue = durationValue; return this; }
        public Builder durationUnit(String durationUnit) { this.durationUnit = durationUnit; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TimeFilterPresetDto build() {
            return new TimeFilterPresetDto(id, name, durationValue, durationUnit, isActive, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDurationValue() { return durationValue; }
    public void setDurationValue(Integer durationValue) { this.durationValue = durationValue; }
    public String getDurationUnit() { return durationUnit; }
    public void setDurationUnit(String durationUnit) { this.durationUnit = durationUnit; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
