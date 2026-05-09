CREATE TABLE hashtags (
    hashtag_id   BIGINT NOT NULL AUTO_INCREMENT,
    tag          VARCHAR(100) NOT NULL,
    post_count   BIGINT NOT NULL DEFAULT 0,
    last_used_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (hashtag_id),
    UNIQUE KEY uk_tag (tag),
    INDEX idx_hashtag_count (post_count DESC)
);

CREATE TABLE post_hashtags (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    post_id    BIGINT NOT NULL,
    hashtag_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_hashtag (post_id, hashtag_id),
    INDEX idx_ph_hashtag (hashtag_id),
    INDEX idx_ph_post (post_id),
    CONSTRAINT fk_ph_hashtag FOREIGN KEY (hashtag_id) REFERENCES hashtags(hashtag_id) ON DELETE CASCADE
);
