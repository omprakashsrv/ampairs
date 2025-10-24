-- Notification Module Database Migration Script
-- Version: 4.10
-- Description: Create notification_queue table for async notification dispatch
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_9__create_workspace_module_tables.sql

-- =====================================================
-- Notification Queue
-- =====================================================
CREATE TABLE notification_queue (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    recipient VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP NULL,
    provider_used VARCHAR(50),
    provider_message_id VARCHAR(255),
    error_message TEXT,
    provider_response TEXT,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_notification_queue_uid (uid),
    INDEX idx_notification_queue_owner (owner_id),
    INDEX idx_notification_queue_status (status),
    INDEX idx_notification_queue_scheduled (scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Queued notifications pending async delivery';
