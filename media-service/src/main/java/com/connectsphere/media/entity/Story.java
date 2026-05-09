package com.connectsphere.media.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Represents the Story domain model.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "stories")
public class Story {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_id") private Long storyId;

    @Column(name = "author_id", nullable = false) private Long authorId;
    @Column(name = "media_url", nullable = false, length = 1000) private String mediaUrl;
    @Column(length = 500) private String caption;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20) @Builder.Default private MediaType mediaType = MediaType.IMAGE;

    @Column(name = "views_count", nullable = false) @Builder.Default private long viewsCount = 0;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "is_active", nullable = false) @Builder.Default private boolean active = true;

    @PrePersist void prePersist() {
        createdAt = Instant.now();
        if (expiresAt == null) expiresAt = createdAt.plusSeconds(86400);
    }
}
