package com.connectsphere.post.dto;

import com.connectsphere.post.entity.PostType;
import com.connectsphere.post.entity.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the payload used for Update Post operations.
 */
public record UpdatePostRequest(
        @NotBlank(message = "content is required")
        @Size(max = 5000, message = "content must be at most 5000 characters")
        String content,

        List<@Size(max = 1000, message = "media URL must be at most 1000 characters") String> mediaUrls,

        PostType postType,

        PostVisibility visibility
) {
    public List<String> safeMediaUrls() {
        return mediaUrls == null ? new ArrayList<>() : mediaUrls;
    }
}
