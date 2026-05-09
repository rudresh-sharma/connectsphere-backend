package com.connectsphere.post.util;

import com.connectsphere.post.client.UserSummary;
import com.connectsphere.post.dto.PostResponse;
import com.connectsphere.post.entity.Post;
import java.util.List;

public final class PostMapper {

    private PostMapper() {
    }

/**
 * Performs the to response operation.
 * @param post method input parameter
 * @param author method input parameter
 * @return resulting value
 */
    public static PostResponse toResponse(Post post, UserSummary author) {
        return new PostResponse(
                post.getPostId(),
                post.getAuthorId(),
                author == null ? null : author.username(),
                author == null ? null : author.fullName(),
                author == null ? null : author.profilePicUrl(),
                post.getContent(),
                List.copyOf(post.getMediaUrls()),
                post.getPostType(),
                post.getVisibility(),
                post.getModerationStatus(),
                post.getModerationReason(),
                post.isAutomatedFlagged(),
                post.getLikesCount(),
                post.getCommentsCount(),
                post.getSharesCount(),
                post.isPromoted(),
                post.getPromotedUntil(),
                post.getPromotionStatus(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
