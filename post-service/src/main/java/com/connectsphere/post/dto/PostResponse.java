package com.connectsphere.post.dto;

import com.connectsphere.post.entity.PostType;
import com.connectsphere.post.entity.PostVisibility;
import com.connectsphere.post.entity.ModerationStatus;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Represents the payload used for Post operations.
 */
public record PostResponse(
        Long postId,
        Long authorId,
        String authorUsername,
        String authorFullName,
        String authorProfilePicUrl,
        String content,
        List<String> mediaUrls,
        PostType postType,
        PostVisibility visibility,
        ModerationStatus moderationStatus,
        String moderationReason,
        boolean automatedFlagged,
        long likesCount,
        long commentsCount,
        long sharesCount,
        boolean promoted,
        Instant promotedUntil,
        String promotionStatus,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
