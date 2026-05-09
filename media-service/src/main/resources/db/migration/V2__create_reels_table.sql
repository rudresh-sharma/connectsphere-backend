CREATE TABLE reels (
    reel_id     BIGINT NOT NULL AUTO_INCREMENT,
    author_id   BIGINT NOT NULL,
    video_url   VARCHAR(1000) NOT NULL,
    caption     VARCHAR(500),
    views_count BIGINT NOT NULL DEFAULT 0,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    is_active   BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (reel_id),
    INDEX idx_reels_active_created (is_active, created_at),
    INDEX idx_reels_author_active (author_id, is_active)
);
