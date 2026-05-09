package com.connectsphere.comment.util;

import com.connectsphere.comment.client.UserSummary;
import com.connectsphere.comment.dto.CommentResponse;
import com.connectsphere.comment.entity.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

/**
 * Performs the to response operation.
 * @param comment method input parameter
 * @param author method input parameter
 * @param replyCount method input parameter
 * @return resulting value
 */
    public static CommentResponse toResponse(Comment comment, UserSummary author, long replyCount) {
        return new CommentResponse(
                comment.getCommentId(),
                comment.getPostId(),
                comment.getAuthorId(),
                author == null ? null : author.username(),
                author == null ? null : author.fullName(),
                author == null ? null : author.profilePicUrl(),
                comment.getParentCommentId(),
                comment.getContent(),
                comment.getLikesCount(),
                replyCount,
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
