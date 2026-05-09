package com.connectsphere.media.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Represents the Reel domain model.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reels")
public class Reel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reel_id")
    private Long reelId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "video_url", nullable = false, length = 1000)
    private String videoUrl;

    @Column(length = 500)
    private String caption;

    @Column(name = "views_count", nullable = false)
    @Builder.Default
    private long viewsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
