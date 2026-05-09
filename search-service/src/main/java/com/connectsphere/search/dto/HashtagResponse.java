package com.connectsphere.search.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the payload used for Hashtag operations.
 */
public record HashtagResponse(Long hashtagId, String tag, long postCount, Instant lastUsedAt) implements Serializable {}
