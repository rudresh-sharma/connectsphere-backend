ALTER TABLE posts
    ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN moderation_reason VARCHAR(500) NULL,
    ADD COLUMN automated_flagged BIT(1) NOT NULL DEFAULT b'0';

CREATE TABLE post_reports (
    report_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    resolved BIT(1) NOT NULL DEFAULT b'0',
    resolution_note VARCHAR(500) NULL,
    resolved_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (report_id),
    INDEX idx_post_reports_post_resolved_created (post_id, resolved, created_at),
    INDEX idx_post_reports_resolved_created (resolved, created_at)
);
