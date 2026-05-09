package com.connectsphere.like.dto;

import com.connectsphere.like.entity.ReactionType;
import com.connectsphere.like.entity.TargetType;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload used for Like operations.
 */
public record LikeRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotNull(message = "targetId is required") Long targetId,
        @NotNull(message = "targetType is required") TargetType targetType,
        ReactionType reactionType
) {}
