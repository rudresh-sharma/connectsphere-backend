package com.connectsphere.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for OAuth Complete operations.
 */
public record OAuthCompleteRequest(
        @NotBlank String setupToken,
        @NotBlank
        @Size(min = 3, max = 40)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
        String username,
        @Size(max = 300) String bio
) {
}
