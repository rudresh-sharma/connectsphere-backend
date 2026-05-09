package com.connectsphere.media.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Create Story operations.
 */
public record CreateStoryRequest(@NotNull Long authorId, @Size(max=500) String caption) {}
