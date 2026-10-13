package com.twekl.dashboard.dto.administration;

import jakarta.validation.constraints.*;

/** Only editable contact fields are bound; IDs, totals and timestamps belong to the server. */
public record CustomerForm(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 20) String phoneNumber,
        @Email @Size(max = 255) String email,
        @Size(max = 255) String city) {}
