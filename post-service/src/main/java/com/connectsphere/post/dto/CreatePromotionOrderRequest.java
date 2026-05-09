package com.connectsphere.post.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload used for Create Promotion Order operations.
 */
public record CreatePromotionOrderRequest(
        @NotNull Long userId,
        @Min(100) Integer amountPaise,
        @Min(1) Integer durationDays
) {
}
