-- Notification Module Database Migration Script (PostgreSQL)
-- Version: 4.10
-- Description: Create notification_queue table for async notification dispatch
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_9__create_workspace_module_tables.sql

-- =====================================================
-- Notification Queue
-- =====================================================
CREATE TABLE notification_queue (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    recipient VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP(6),
    provider_used VARCHAR(50),
    provider_message_id VARCHAR(255),
    error_message TEXT,
    provider_response TEXT
);

CREATE INDEX idx_notification_queue_owner ON notification_queue(owner_id);
CREATE INDEX idx_notification_queue_status ON notification_queue(status);
CREATE INDEX idx_notification_queue_scheduled ON notification_queue(scheduled_at);

COMMENT ON TABLE notification_queue IS 'Queued notifications pending async delivery';
