package com.twekl.dashboard.model.timefilter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Pure JPA Entity representing Follow-up Time Filter Milestones/Presets.
 */
@Entity
@Table(name = "TIME_FILTER_PRESETS")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TimeFilterPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "Preset key is required")
    @Column(name = "PRESET_KEY", nullable = false, unique = true, length = 32)
    private String presetKey;

    @NotBlank(message = "English label is required")
    @Column(name = "LABEL_EN", nullable = false, length = 80)
    private String labelEn;

    @Column(name = "LABEL_AR", length = 80)
    private String labelAr;

    @Column(name = "LABEL_KU", length = 80)
    private String labelKu;

    @Column(name = "DURATION_VALUE", nullable = false)
    private int durationValue = 1;

    @Column(name = "DURATION_UNIT", nullable = false, length = 10)
    private String durationUnit = "DAYS"; // HOURS, DAYS, WEEKS, MONTHS

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "DISPLAY_ORDER", nullable = false)
    private int displayOrder = 0;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    public TimeFilterPreset() {}

    public TimeFilterPreset(String presetKey, String labelEn, String labelAr, String labelKu, int durationValue, String durationUnit, int displayOrder) {
        this.presetKey = presetKey;
        this.labelEn = labelEn;
        this.labelAr = labelAr;
        this.labelKu = labelKu;
        this.durationValue = durationValue;
        this.durationUnit = (durationUnit != null) ? durationUnit : "DAYS";
        this.displayOrder = displayOrder;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPresetId() { return id; }
    public void setPresetId(Long presetId) { this.id = presetId; }

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

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Compatibility aliases
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getName() { return labelEn; }
    public void setName(String name) { this.labelEn = name; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean getIsActive() { return active; }
    public void setIsActive(boolean isActive) { this.active = isActive; }

    public int getTargetDays() {
        if ("HOURS".equalsIgnoreCase(durationUnit)) return Math.max(1, durationValue / 24);
        if ("WEEKS".equalsIgnoreCase(durationUnit)) return durationValue * 7;
        if ("MONTHS".equalsIgnoreCase(durationUnit)) return durationValue * 30;
        return durationValue;
    }

    public int getEquivalentDurationMinutes() {
        String u = durationUnit != null ? durationUnit.toUpperCase() : "DAYS";
        int v = durationValue > 0 ? durationValue : 1;
        return switch (u) {
            case "HOURS" -> v * 60;
            case "WEEKS" -> v * 7 * 1440;
            case "MONTHS" -> v * 30 * 1440;
            default -> v * 1440;
        };
    }
}

