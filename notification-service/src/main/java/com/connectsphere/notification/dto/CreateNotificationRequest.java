package com.connectsphere.notification.dto;

import com.connectsphere.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Create Notification operations.
 */
public record CreateNotificationRequest(
        @NotNull Long recipientId,
        @NotNull Long actorId,
        @NotNull NotificationType type,
        @Size(max = 500) String message,
        Long targetId,
        String targetType
) {}
