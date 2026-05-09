package com.connectsphere.follow.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload used for Follow operations.
 */
public record FollowRequest(
        @NotNull(message = "followerId is required")
        Long followerId
) {
}
