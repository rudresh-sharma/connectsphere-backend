package com.connectsphere.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload used for Create Razorpay Order operations.
 */
public record CreateRazorpayOrderRequest(
        @NotNull @Min(100) Integer amountPaise,
        @NotBlank String receipt
) {
}
