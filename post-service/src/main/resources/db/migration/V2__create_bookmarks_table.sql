CREATE TABLE bookmarks (
    bookmark_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bookmark_id),
    CONSTRAINT uk_bookmarks_user_post UNIQUE (user_id, post_id),
    INDEX idx_bookmarks_user_created (user_id, created_at),
    INDEX idx_bookmarks_post (post_id)
);
