package com.connectsphere.media.client;

/**
 * Declares the remote contract for User Summary integration.
 */
public record UserSummary(Long userId, String username, String fullName, String email, String profilePicUrl) {}
