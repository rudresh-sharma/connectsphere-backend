package com.connectsphere.comment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Resolve Comment Report operations.
 */
public record ResolveCommentReportRequest(
        @NotNull Long adminUserId,
        boolean removeComment,
        @Size(max = 500) String resolutionNote
) {
}
