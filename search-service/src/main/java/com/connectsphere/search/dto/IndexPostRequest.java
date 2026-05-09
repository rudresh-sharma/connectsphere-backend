package com.connectsphere.search.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload used for Index Post operations.
 */
public record IndexPostRequest(@NotNull Long postId, @NotNull Long authorId, String content, String authorUsername, String visibility) {}
