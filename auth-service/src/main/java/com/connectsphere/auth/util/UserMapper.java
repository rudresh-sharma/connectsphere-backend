package com.connectsphere.auth.util;

import com.connectsphere.auth.dto.PublicUserResponse;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

/**
 * Performs the to response operation.
 * @param user method input parameter
 * @return resulting value
 */
    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getBio(),
                user.getProfilePicUrl(),
                user.getRole(),
                user.getProvider(),
                user.getProviderId(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

/**
 * Performs the to public response operation.
 * @param user method input parameter
 * @return resulting value
 */
    public static PublicUserResponse toPublicResponse(User user) {
        return new PublicUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getBio(),
                user.getProfilePicUrl(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
