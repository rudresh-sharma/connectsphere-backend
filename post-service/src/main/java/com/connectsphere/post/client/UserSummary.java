package com.connectsphere.post.client;

/**
 * Declares the remote contract for User Summary integration.
 */
public record UserSummary(
        Long userId,
        String username,
        String fullName,
        String profilePicUrl,
        String email,
        String role,
        boolean active
) {
}
