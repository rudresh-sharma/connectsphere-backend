package com.connectsphere.like.dto;

import com.connectsphere.like.entity.ReactionType;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload used for Change Reaction operations.
 */
public record ChangeReactionRequest(
        @NotNull(message = "reactionType is required") ReactionType reactionType
) {}
