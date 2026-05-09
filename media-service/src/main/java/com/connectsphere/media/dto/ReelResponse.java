package com.connectsphere.media.dto;

import java.time.Instant;

/**
 * Represents the payload used for Reel operations.
 */
public record ReelResponse(
        Long reelId,
        Long authorId,
        String videoUrl,
        String caption,
        long viewsCount,
        Instant createdAt,
        String authorUsername,
        String authorFullName,
        String authorProfilePicUrl
) {}
