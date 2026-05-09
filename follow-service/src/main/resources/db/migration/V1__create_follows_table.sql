CREATE TABLE follows (
    follow_id BIGINT NOT NULL AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (follow_id),
    CONSTRAINT uk_follows_follower_following UNIQUE (follower_id, following_id),
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> following_id),
    INDEX idx_follows_follower_created (follower_id, created_at),
    INDEX idx_follows_following_created (following_id, created_at)
);
