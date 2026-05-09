package com.connectsphere.follow.dto;

import java.io.Serializable;

/**
 * Represents the payload used for Follow Status operations.
 */
public record FollowStatusResponse(
        Long followerId,
        Long followingId,
        boolean following
) implements Serializable {
}
