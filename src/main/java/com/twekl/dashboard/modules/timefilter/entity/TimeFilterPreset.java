package com.twekl.dashboard.modules.timefilter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_filter_presets")
public class TimeFilterPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Preset key is required")
    @Column(nullable = false, unique = true, length = 64)
    private String presetKey; // e.g. "24_HOURS", "7_DAYS", "30_DAYS"

    @NotBlank(message = "English label is required")
    @Column(nullable = false, length = 100)
    private String labelEn;

    @Column(length = 100)
    private String labelAr;

    @Column(length = 100)
    private String labelKu;

    private int durationValue = 1;
    private String durationUnit = "DAYS"; // HOURS, DAYS, WEEKS, MONTHS

    private boolean active = true;
    private int displayOrder = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public TimeFilterPreset() {
        this.createdAt = LocalDateTime.now();
    }

    public TimeFilterPreset(String presetKey, String labelEn, String labelAr, String labelKu, int durationValue, String durationUnit, int displayOrder) {
        this.presetKey = presetKey;
        this.labelEn = labelEn;
        this.labelAr = labelAr;
        this.labelKu = labelKu;
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
        this.displayOrder = displayOrder;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPresetKey() { return presetKey; }
    public void setPresetKey(String presetKey) { this.presetKey = presetKey; }

    public String getLabelEn() { return labelEn; }
    public void setLabelEn(String labelEn) { this.labelEn = labelEn; }

    public String getLabelAr() { return labelAr; }
    public void setLabelAr(String labelAr) { this.labelAr = labelAr; }

    public String getLabelKu() { return labelKu; }
    public void setLabelKu(String labelKu) { this.labelKu = labelKu; }

    public int getDurationValue() { return durationValue; }
    public void setDurationValue(int durationValue) { this.durationValue = durationValue; }

    public String getDurationUnit() { return durationUnit; }
    public void setDurationUnit(String durationUnit) { this.durationUnit = durationUnit; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // Serialization compatibility aliases for Layer 7 JSON contract
    public String getName() { return labelEn; }
    public boolean getIsActive() { return active; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
