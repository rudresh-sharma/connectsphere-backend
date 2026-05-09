package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * Represents the payload used for Update Profile operations.
 */
public record UpdateProfileRequest(
        @Size(min = 3, max = 40)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
        String username,
        @Size(max = 120) String fullName,
        @Size(max = 300) String bio,
        String profilePicUrl
) {
}
