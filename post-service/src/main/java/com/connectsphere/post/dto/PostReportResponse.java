package com.connectsphere.post.dto;

import java.time.Instant;

/**
 * Represents the payload used for Post Report operations.
 */
public record PostReportResponse(
        Long reportId,
        Long postId,
        Long reporterId,
        String reporterUsername,
        String reason,
        boolean resolved,
        String resolutionNote,
        Long resolvedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
