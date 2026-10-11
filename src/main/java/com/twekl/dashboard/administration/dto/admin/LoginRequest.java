package com.twekl.dashboard.administration.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * Immutable login credentials carrier using Java 21 record.
 */
public record LoginRequest(
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Password is required") String password
) {
    public String getUsername() { return username(); }
    public String getPassword() { return password(); }
}

