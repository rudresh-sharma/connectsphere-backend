package com.connectsphere.payment.dto;

/**
 * Represents the payload used for Verify Razorpay Payment operations.
 */
public record VerifyRazorpayPaymentResponse(boolean valid) {
}
