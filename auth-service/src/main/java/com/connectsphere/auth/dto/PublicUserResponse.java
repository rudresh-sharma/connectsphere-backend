package com.connectsphere.auth.dto;

import com.connectsphere.auth.entity.Role;
import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the payload used for Public User operations.
 */
public record PublicUserResponse(
        Long userId,
        String username,
        String fullName,
        String bio,
        String profilePicUrl,
        String email,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
