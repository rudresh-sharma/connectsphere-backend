package com.connectsphere.notification.dto;

import java.util.List;

/**
 * Represents the payload used for Bulk Notification operations.
 */
public record BulkNotificationRequest(
        List<Long> recipientIds,
        String message
) {}
