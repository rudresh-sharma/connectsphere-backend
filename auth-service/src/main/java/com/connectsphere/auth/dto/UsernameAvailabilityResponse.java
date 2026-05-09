package com.connectsphere.auth.dto;

/**
 * Represents the payload used for Username Availability operations.
 */
public record UsernameAvailabilityResponse(
        String username,
        boolean available
) {
}
