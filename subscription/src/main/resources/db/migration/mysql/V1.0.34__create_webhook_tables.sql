-- Migration: V1.0.34 - Create webhook idempotency and logging tables
-- Purpose: Enable webhook deduplication and replay mechanism for payment providers
-- Date: 2025-01-27

-- =====================================================
-- Webhook Events Table (Idempotency)
-- =====================================================
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    processed_at TIMESTAMP NOT NULL,
    external_subscription_id VARCHAR(255),
    workspace_id VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX idx_webhook_provider_event_id (provider, event_id),
    INDEX idx_webhook_event_uid (uid),
    INDEX idx_webhook_processed_at (processed_at),
    INDEX idx_webhook_external_subscription_id (external_subscription_id),
    INDEX idx_webhook_workspace_id (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Webhook Logs Table (For Debugging and Replay)
-- =====================================================
CREATE TABLE IF NOT EXISTS webhook_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL,
    payload TEXT NOT NULL,
    signature VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NULL,
    headers TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_webhook_log_uid (uid),
    INDEX idx_webhook_log_provider (provider),
    INDEX idx_webhook_log_status (status),
    INDEX idx_webhook_log_received_at (received_at),
    INDEX idx_webhook_log_next_retry_at (next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Comments
-- =====================================================
ALTER TABLE webhook_events COMMENT = 'Tracks processed webhook events to ensure idempotency';
ALTER TABLE webhook_logs COMMENT = 'Logs all incoming webhooks for debugging and replay';
