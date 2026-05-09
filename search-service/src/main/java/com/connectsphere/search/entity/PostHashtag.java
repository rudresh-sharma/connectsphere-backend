package com.connectsphere.search.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Represents the Post Hashtag domain model.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "post_hashtags")
public class PostHashtag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false) private Long postId;

    @Column(name = "hashtag_id", nullable = false) private Long hashtagId;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
