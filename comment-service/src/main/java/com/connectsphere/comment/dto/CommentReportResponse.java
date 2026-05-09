package com.connectsphere.comment.dto;

import java.time.Instant;

/**
 * Represents the payload used for Comment Report operations.
 */
public record CommentReportResponse(
        Long reportId,
        Long commentId,
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
