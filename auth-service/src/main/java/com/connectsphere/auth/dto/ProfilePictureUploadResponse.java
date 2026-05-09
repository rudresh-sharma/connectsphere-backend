package com.connectsphere.auth.dto;

/**
 * Represents the payload used for Profile Picture Upload operations.
 */
public record ProfilePictureUploadResponse(
        String profilePicUrl,
        String publicId
) {
}
