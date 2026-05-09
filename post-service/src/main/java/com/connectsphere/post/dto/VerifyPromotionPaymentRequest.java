package com.connectsphere.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload used for Verify Promotion Payment operations.
 */
public record VerifyPromotionPaymentRequest(
        @NotNull Long userId,
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
) {
}
