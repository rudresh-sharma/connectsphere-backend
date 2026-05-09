package com.connectsphere.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents the payload used for Login operations.
 */
public record LoginRequest(
        @NotBlank String emailOrUsername,
        @NotBlank String password
) {
}

