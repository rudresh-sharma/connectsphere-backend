package com.connectsphere.search.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Represents the Hashtag domain model.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "hashtags")
public class Hashtag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hashtag_id") private Long hashtagId;

    @Column(nullable = false, unique = true, length = 100) private String tag;

    @Column(name = "post_count", nullable = false) @Builder.Default private long postCount = 0;

    @Column(name = "last_used_at", nullable = false) private Instant lastUsedAt;

    @PrePersist void prePersist() { if (lastUsedAt == null) lastUsedAt = Instant.now(); }
}
