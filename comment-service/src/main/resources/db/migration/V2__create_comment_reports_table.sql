CREATE TABLE comment_reports (
    report_id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    resolved BIT(1) NOT NULL DEFAULT b'0',
    resolution_note VARCHAR(500) NULL,
    resolved_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (report_id),
    INDEX idx_comment_reports_comment_resolved_created (comment_id, resolved, created_at),
    INDEX idx_comment_reports_resolved_created (resolved, created_at)
);
