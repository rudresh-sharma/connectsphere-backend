package com.connectsphere.post.dto;

import com.connectsphere.post.entity.ModerationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Admin Moderate Post operations.
 */
public record AdminModeratePostRequest(
        @NotNull Long adminUserId,
        @NotNull ModerationStatus moderationStatus,
        @Size(max = 500) String moderationReason
) {
}
