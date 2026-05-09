package com.connectsphere.post.client;

/**
 * Declares the remote contract for Payment Receipt Email integration.
 */
public record PaymentReceiptEmailRequest(
        Long recipientId,
        String email,
        String username,
        String fullName,
        Long postId,
        String razorpayOrderId,
        String razorpayPaymentId,
        Integer amountPaise,
        Integer durationDays
) {
}
