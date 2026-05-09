CREATE TABLE media (
    media_id       BIGINT NOT NULL AUTO_INCREMENT,
    uploader_id    BIGINT NOT NULL,
    url            VARCHAR(1000) NOT NULL,
    media_type     VARCHAR(20) NOT NULL,
    size_kb        BIGINT NOT NULL DEFAULT 0,
    mime_type      VARCHAR(100),
    linked_post_id BIGINT NULL,
    uploaded_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    is_deleted     BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (media_id),
    INDEX idx_media_uploader (uploader_id),
    INDEX idx_media_post (linked_post_id)
);

CREATE TABLE stories (
    story_id    BIGINT NOT NULL AUTO_INCREMENT,
    author_id   BIGINT NOT NULL,
    media_url   VARCHAR(1000) NOT NULL,
    caption     VARCHAR(500),
    media_type  VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    views_count BIGINT NOT NULL DEFAULT 0,
    expires_at  DATETIME(6) NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    is_active   BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (story_id),
    INDEX idx_stories_author_active (author_id, is_active),
    INDEX idx_stories_expires (expires_at)
);
