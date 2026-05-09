CREATE TABLE notifications (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id    BIGINT NOT NULL,
    actor_id        BIGINT NOT NULL,
    type            VARCHAR(30) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    target_id       BIGINT NULL,
    target_type     VARCHAR(30) NULL,
    is_read         BIT(1) NOT NULL DEFAULT b'0',
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (notification_id),
    INDEX idx_notif_recipient_read (recipient_id, is_read),
    INDEX idx_notif_recipient_created (recipient_id, created_at)
);
