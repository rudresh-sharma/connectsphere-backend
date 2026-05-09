CREATE TABLE likes (
    like_id       BIGINT NOT NULL AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    target_id     BIGINT NOT NULL,
    target_type   VARCHAR(20) NOT NULL,
    reaction_type VARCHAR(20) NOT NULL DEFAULT 'LIKE',
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (like_id),
    UNIQUE KEY uk_user_target (user_id, target_id, target_type),
    INDEX idx_likes_target (target_id, target_type),
    INDEX idx_likes_user (user_id)
);
