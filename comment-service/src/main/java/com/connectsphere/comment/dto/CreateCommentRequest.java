package com.connectsphere.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Create Comment operations.
 */
public record CreateCommentRequest(
        @NotNull(message = "postId is required")
        Long postId,

        @NotNull(message = "authorId is required")
        Long authorId,

        Long parentCommentId,

        @NotBlank(message = "content is required")
        @Size(max = 2000, message = "content must be at most 2000 characters")
        String content
) {
}
