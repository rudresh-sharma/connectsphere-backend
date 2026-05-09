package com.connectsphere.media.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Represents the Media domain model.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "media")
public class Media {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id") private Long mediaId;

    @Column(name = "uploader_id", nullable = false) private Long uploaderId;
    @Column(nullable = false, length = 1000) private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20) private MediaType mediaType;

    @Column(name = "size_kb") @Builder.Default private long sizeKb = 0;
    @Column(name = "mime_type", length = 100) private String mimeType;
    @Column(name = "linked_post_id") private Long linkedPostId;

    @Column(name = "uploaded_at", nullable = false, updatable = false) private Instant uploadedAt;
    @Column(name = "is_deleted", nullable = false) @Builder.Default private boolean deleted = false;

    @PrePersist void prePersist() { uploadedAt = Instant.now(); }
}
