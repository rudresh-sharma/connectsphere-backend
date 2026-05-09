ALTER TABLE posts
    ADD COLUMN promoted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN promoted_until TIMESTAMP NULL,
    ADD COLUMN promotion_order_id VARCHAR(120) NULL,
    ADD COLUMN promotion_payment_id VARCHAR(120) NULL,
    ADD COLUMN promotion_amount_paise INT NULL,
    ADD COLUMN promotion_duration_days INT NULL,
    ADD COLUMN promotion_status VARCHAR(24) NOT NULL DEFAULT 'NONE';

CREATE INDEX idx_posts_promoted_until ON posts (promoted, promoted_until);
