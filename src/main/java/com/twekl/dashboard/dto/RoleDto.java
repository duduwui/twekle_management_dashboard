package com.twekl.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {

    private Long id;

    @NotBlank(message = "Role name is required")
    @Size(max = 64, message = "Role name must not exceed 64 characters")
    private String name;

    @Builder.Default
    private boolean canCreate = false;

    @Builder.Default
    private boolean canRead = true;

    @Builder.Default
    private boolean canUpdate = false;

    @Builder.Default
    private boolean canDelete = false;
}
