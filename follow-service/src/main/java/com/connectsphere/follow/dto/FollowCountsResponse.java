package com.connectsphere.follow.dto;

import java.io.Serializable;

/**
 * Represents the payload used for Follow Counts operations.
 */
public record FollowCountsResponse(
        Long userId,
        long followersCount,
        long followingCount
) implements Serializable {
}
