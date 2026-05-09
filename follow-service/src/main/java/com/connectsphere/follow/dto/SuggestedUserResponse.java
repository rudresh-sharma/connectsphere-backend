package com.connectsphere.follow.dto;

/**
 * Represents the payload used for Suggested User operations.
 */
public record SuggestedUserResponse(
        Long userId,
        long mutualConnections
) {
}
