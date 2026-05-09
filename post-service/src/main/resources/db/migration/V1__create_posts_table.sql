CREATE TABLE posts (
    post_id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    post_type VARCHAR(30) NOT NULL,
    visibility VARCHAR(30) NOT NULL,
    likes_count BIGINT NOT NULL DEFAULT 0,
    comments_count BIGINT NOT NULL DEFAULT 0,
    shares_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    is_deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (post_id),
    INDEX idx_posts_author_deleted_created (author_id, is_deleted, created_at),
    INDEX idx_posts_visibility_deleted_created (visibility, is_deleted, created_at)
);

CREATE TABLE post_media_urls (
    post_id BIGINT NOT NULL,
    media_url VARCHAR(1000) NOT NULL,
    INDEX idx_post_media_urls_post_id (post_id),
    CONSTRAINT fk_post_media_urls_post FOREIGN KEY (post_id) REFERENCES posts (post_id)
);
