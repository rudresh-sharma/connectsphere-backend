package com.connectsphere.media.dto;

import com.connectsphere.media.entity.MediaType;
import java.time.Instant;

/**
 * Represents the payload used for Story operations.
 */
public record StoryResponse(Long storyId, Long authorId, String mediaUrl, String caption, MediaType mediaType, long viewsCount, Instant expiresAt, Instant createdAt, String authorUsername, String authorProfilePicUrl) {}
