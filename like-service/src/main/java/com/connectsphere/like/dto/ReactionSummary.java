package com.connectsphere.like.dto;

import com.connectsphere.like.entity.ReactionType;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents the payload used for Reaction Summary operations.
 */
public record ReactionSummary(
        Long targetId,
        long totalCount,
        Map<ReactionType, Long> counts
) implements Serializable {}
