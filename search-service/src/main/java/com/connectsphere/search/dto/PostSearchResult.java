package com.connectsphere.search.dto;

import java.io.Serializable;

/**
 * Represents the payload used for Post Search Result operations.
 */
public record PostSearchResult(Long postId, Long authorId, String content, String authorUsername, String visibility)
        implements Serializable {}
