package com.connectsphere.media.dto;

import com.connectsphere.media.entity.MediaType;
import java.time.Instant;

/**
 * Represents the payload used for Media operations.
 */
public record MediaResponse(Long mediaId, Long uploaderId, String url, MediaType mediaType, long sizeKb, String mimeType, Long linkedPostId, Instant uploadedAt) {}
