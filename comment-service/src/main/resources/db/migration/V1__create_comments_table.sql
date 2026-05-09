CREATE TABLE comments (
    comment_id        BIGINT NOT NULL AUTO_INCREMENT,
    post_id           BIGINT NOT NULL,
    author_id         BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    content           TEXT NOT NULL,
    likes_count       BIGINT NOT NULL DEFAULT 0,
    is_deleted        BIT(1) NOT NULL DEFAULT b'0',
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (comment_id),
    INDEX idx_comments_post_deleted_created (post_id, is_deleted, created_at),
    INDEX idx_comments_author (author_id),
    INDEX idx_comments_parent (parent_comment_id)
);
