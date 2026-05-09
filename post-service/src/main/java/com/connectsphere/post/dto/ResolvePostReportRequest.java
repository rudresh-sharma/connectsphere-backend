package com.connectsphere.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Resolve Post Report operations.
 */
public record ResolvePostReportRequest(
        @NotNull Long adminUserId,
        boolean removePost,
        @Size(max = 500) String resolutionNote
) {
}
