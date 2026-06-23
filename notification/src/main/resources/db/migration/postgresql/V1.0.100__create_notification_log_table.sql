-- Notification Module Migration (PostgreSQL)
-- Version: 1.0.100
-- Description: In-app notification center history feed (canonical /sync resource)

CREATE TABLE notification_log (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data_payload TEXT,
    target_user_id VARCHAR(200),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_log_owner ON notification_log(owner_id);
CREATE INDEX idx_notification_log_owner_updated ON notification_log(owner_id, updated_at);

COMMENT ON TABLE notification_log IS 'In-app notification center history (workspace-scoped, /sync feed)';
