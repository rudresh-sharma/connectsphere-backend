package com.connectsphere.auth.dto;

/**
 * Represents the payload used for Auth operations.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
}

