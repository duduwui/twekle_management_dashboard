package com.twekl.dashboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
}
