package com.connectsphere.comment.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the payload used for Comment operations.
 */
public record CommentResponse(
        Long commentId,
        Long postId,
        Long authorId,
        String authorUsername,
        String authorFullName,
        String authorProfilePicUrl,
        Long parentCommentId,
        String content,
        long likesCount,
        long replyCount,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
