package com.connectsphere.post.dto;

/**
 * Represents the payload used for Media Upload operations.
 */
public record MediaUploadResponse(
        String url,
        String publicId,
        String resourceType,
        Long bytes,
        Double durationSeconds
) {
}
