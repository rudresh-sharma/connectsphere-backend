package com.connectsphere.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Post Report operations.
 */
public record PostReportRequest(
        @NotNull Long reporterId,
        @NotBlank @Size(max = 500) String reason
) {
}
