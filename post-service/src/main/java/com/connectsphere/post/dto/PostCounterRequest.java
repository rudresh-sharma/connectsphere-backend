package com.connectsphere.post.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents the payload used for Post Counter operations.
 */
public record PostCounterRequest(
        @NotBlank(message = "counterType is required")
        String counterType,
        long delta
) {
}
