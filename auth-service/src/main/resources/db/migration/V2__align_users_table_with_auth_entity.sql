ALTER TABLE users
    MODIFY COLUMN password_hash VARCHAR(255) NULL;

ALTER TABLE users
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

SET @updated_at_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'updated_at'
);

SET @add_updated_at_sql = IF(
    @updated_at_exists = 0,
    'ALTER TABLE users ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);

PREPARE add_updated_at_statement FROM @add_updated_at_sql;
EXECUTE add_updated_at_statement;
DEALLOCATE PREPARE add_updated_at_statement;
