package com.connectsphere.post.client;

/**
 * Declares the remote contract for Index Post integration.
 */
public record IndexPostRequest(
        Long postId,
        Long authorId,
        String content,
        String authorUsername,
        String visibility
) {}
