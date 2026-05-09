package com.connectsphere.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Comment Report operations.
 */
public record CommentReportRequest(
        @NotNull Long reporterId,
        @NotBlank @Size(max = 500) String reason
) {
}
