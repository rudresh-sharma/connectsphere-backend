package com.connectsphere.like.dto;

import com.connectsphere.like.entity.ReactionType;
import com.connectsphere.like.entity.TargetType;
import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the payload used for Like operations.
 */
public record LikeResponse(
        Long likeId,
        Long userId,
        Long targetId,
        TargetType targetType,
        ReactionType reactionType,
        Instant createdAt
) implements Serializable {}
