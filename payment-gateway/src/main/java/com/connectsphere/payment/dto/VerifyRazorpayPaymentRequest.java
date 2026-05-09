package com.connectsphere.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents the payload used for Verify Razorpay Payment operations.
 */
public record VerifyRazorpayPaymentRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
) {
}
