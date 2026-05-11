package com.connectsphere.post.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.connectsphere.post.client.UserSummary;
import com.connectsphere.post.dto.PostResponse;
import com.connectsphere.post.entity.ModerationStatus;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.entity.PostType;
import com.connectsphere.post.entity.PostVisibility;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class PostMapperTest {

    @Test
    void toResponseCopiesAuthorAndPostFields() {
        Instant now = Instant.now();
        Post post = Post.builder()
                .postId(13L)
                .authorId(2L)
                .content("hello")
                .mediaUrls(new ArrayList<>(java.util.List.of("https://cdn.test/post.jpg")))
                .postType(PostType.IMAGE)
                .visibility(PostVisibility.PUBLIC)
                .moderationStatus(ModerationStatus.APPROVED)
                .likesCount(3)
                .commentsCount(4)
                .sharesCount(1)
                .promotionStatus("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        PostResponse response = PostMapper.toResponse(post, new UserSummary(2L, "anuj", "Anuj", "avatar.jpg", "a@example.com", "USER", true));

        assertEquals(13L, response.postId());
        assertEquals("anuj", response.authorUsername());
        assertEquals("avatar.jpg", response.authorProfilePicUrl());
        assertEquals(3L, response.likesCount());
    }

    @Test
    void toResponseReturnsImmutableMediaUrls() {
        Post post = Post.builder()
                .postId(13L)
                .authorId(2L)
                .content("hello")
                .mediaUrls(new ArrayList<>(java.util.List.of("https://cdn.test/post.jpg")))
                .postType(PostType.IMAGE)
                .visibility(PostVisibility.PUBLIC)
                .moderationStatus(ModerationStatus.APPROVED)
                .promotionStatus("NONE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PostResponse response = PostMapper.toResponse(post, null);
        var mediaUrls = response.mediaUrls();

        assertThrows(UnsupportedOperationException.class, () -> mediaUrls.add("new-url"));
    }
}
