package com.connectsphere.post.dto;

/**
 * Represents the payload used for Create Promotion Order operations.
 */
public record CreatePromotionOrderResponse(
        String keyId,
        String orderId,
        int amountPaise,
        String currency,
        Long postId,
        int durationDays
) {
}
