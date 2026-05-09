package com.connectsphere.payment.dto;

/**
 * Represents the payload used for Create Razorpay Order operations.
 */
public record CreateRazorpayOrderResponse(
        String keyId,
        String orderId,
        int amountPaise,
        String currency
) {
}
