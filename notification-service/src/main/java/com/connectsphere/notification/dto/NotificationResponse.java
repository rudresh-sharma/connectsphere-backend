package com.connectsphere.notification.dto;

import com.connectsphere.notification.entity.NotificationType;
import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the payload used for Notification operations.
 */
public record NotificationResponse(
        Long notificationId,
        Long recipientId,
        Long actorId,
        NotificationType type,
        String message,
        Long targetId,
        String targetType,
        boolean read,
        Instant createdAt
) implements Serializable {}
