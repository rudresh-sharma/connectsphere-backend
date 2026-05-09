package com.connectsphere.notification.client;

/**
 * Declares the remote contract for User Summary integration.
 */
public record UserSummary(Long userId, String username, String fullName, String email, String profilePicUrl, boolean active) {}
