package com.connectsphere.auth.security;

import com.connectsphere.auth.entity.Provider;

/**
 * Handles Pending OAuth Signup security responsibilities.
 */
public record PendingOAuthSignup(
        Provider provider,
        String providerId,
        String email,
        String fullName,
        String profilePicUrl
) {
}
