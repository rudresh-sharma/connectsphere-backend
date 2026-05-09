package com.connectsphere.follow.dto;

import java.time.Instant;

/**
 * Represents the payload used for Follow operations.
 */
public record FollowResponse(
        Long followId,
        Long followerId,
        Long followingId,
        Instant createdAt
) {
}
