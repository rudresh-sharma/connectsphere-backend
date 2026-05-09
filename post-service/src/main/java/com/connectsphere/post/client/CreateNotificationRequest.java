package com.connectsphere.post.client;

/**
 * Declares the remote contract for Create Notification integration.
 */
public record CreateNotificationRequest(
        Long recipientId,
        Long actorId,
        NotificationType type,
        String message,
        Long targetId,
        String targetType
) {
}
