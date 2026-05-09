package com.connectsphere.notification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Payment Receipt Email operations.
 */
public record PaymentReceiptEmailRequest(
        @NotNull Long recipientId,
        @NotBlank @Size(max = 120) String email,
        @NotBlank @Size(max = 120) String username,
        @NotBlank @Size(max = 120) String fullName,
        @NotNull Long postId,
        @NotBlank @Size(max = 120) String razorpayOrderId,
        @NotBlank @Size(max = 120) String razorpayPaymentId,
        @NotNull @Min(1) Integer amountPaise,
        @NotNull @Min(1) Integer durationDays
) {
}
