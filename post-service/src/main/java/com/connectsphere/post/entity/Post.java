package com.connectsphere.post.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the Post domain model.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "media_url", nullable = false, length = 1000)
    private List<String> mediaUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus;

    @Column(name = "moderation_reason", length = 500)
    private String moderationReason;

    @Column(name = "automated_flagged", nullable = false)
    private boolean automatedFlagged;

    @Column(name = "likes_count", nullable = false)
    private long likesCount;

    @Column(name = "comments_count", nullable = false)
    private long commentsCount;

    @Column(name = "shares_count", nullable = false)
    private long sharesCount;

    @Column(nullable = false)
    private boolean promoted;

    @Column(name = "promoted_until")
    private Instant promotedUntil;

    @Column(name = "promotion_order_id", length = 120)
    private String promotionOrderId;

    @Column(name = "promotion_payment_id", length = 120)
    private String promotionPaymentId;

    @Column(name = "promotion_amount_paise")
    private Integer promotionAmountPaise;

    @Column(name = "promotion_duration_days")
    private Integer promotionDurationDays;

    @Column(name = "promotion_status", nullable = false, length = 24)
    private String promotionStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (postType == null) {
            postType = PostType.TEXT;
        }
        if (visibility == null) {
            visibility = PostVisibility.PUBLIC;
        }
        if (moderationStatus == null) {
            moderationStatus = ModerationStatus.APPROVED;
        }
        if (promotionStatus == null) {
            promotionStatus = "NONE";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
