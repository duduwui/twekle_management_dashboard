package com.twekl.dashboard.modules.timefilter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class TimeFilterPresetDto {

    @NotBlank(message = "Preset key is required")
    private String presetKey;

    @NotBlank(message = "English label is required")
    private String labelEn;

    private String labelAr;
    private String labelKu;

    @Positive(message = "Duration value must be positive")
    private int durationValue = 1;

    @NotBlank(message = "Duration unit is required")
    private String durationUnit = "DAYS";

    private boolean active = true;
    private int displayOrder = 0;

    public TimeFilterPresetDto() {}

    public TimeFilterPresetDto(String presetKey, String labelEn, String labelAr, String labelKu, int durationValue, String durationUnit, int displayOrder) {
        this.presetKey = presetKey;
        this.labelEn = labelEn;
        this.labelAr = labelAr;
        this.labelKu = labelKu;
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
        this.displayOrder = displayOrder;
        this.active = true;
    }

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
}
