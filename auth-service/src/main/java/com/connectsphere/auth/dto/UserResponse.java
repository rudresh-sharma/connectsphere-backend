package com.connectsphere.auth.dto;

import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.entity.Role;
import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the payload used for User operations.
 */
public record UserResponse(
        Long userId,
        String username,
        String email,
        String fullName,
        String bio,
        String profilePicUrl,
        Role role,
        Provider provider,
        String providerId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
