-- Migration: V1.0.34 - Create webhook idempotency and logging tables
-- Purpose: Enable webhook deduplication and replay mechanism for payment providers
-- Date: 2025-01-27

-- =====================================================
-- Webhook Events Table (Idempotency)
-- =====================================================
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    processed_at TIMESTAMP NOT NULL,
    external_subscription_id VARCHAR(255),
    workspace_id VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint on provider + event_id
CREATE UNIQUE INDEX IF NOT EXISTS idx_webhook_provider_event_id ON webhook_events(provider, event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_event_uid ON webhook_events(uid);
CREATE INDEX IF NOT EXISTS idx_webhook_processed_at ON webhook_events(processed_at);
CREATE INDEX IF NOT EXISTS idx_webhook_external_subscription_id ON webhook_events(external_subscription_id);
CREATE INDEX IF NOT EXISTS idx_webhook_workspace_id ON webhook_events(workspace_id);

-- Table comment
COMMENT ON TABLE webhook_events IS 'Tracks processed webhook events to ensure idempotency';

-- =====================================================
-- Webhook Logs Table (For Debugging and Replay)
-- =====================================================
CREATE TABLE IF NOT EXISTS webhook_logs (
    id BIGSERIAL PRIMARY KEY,
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webhook_log_uid ON webhook_logs(uid);
CREATE INDEX IF NOT EXISTS idx_webhook_log_provider ON webhook_logs(provider);
CREATE INDEX IF NOT EXISTS idx_webhook_log_status ON webhook_logs(status);
CREATE INDEX IF NOT EXISTS idx_webhook_log_received_at ON webhook_logs(received_at);
CREATE INDEX IF NOT EXISTS idx_webhook_log_next_retry_at ON webhook_logs(next_retry_at);

-- Table comment
COMMENT ON TABLE webhook_logs IS 'Logs all incoming webhooks for debugging and replay';

-- =====================================================
-- Trigger for updated_at timestamp (PostgreSQL)
-- =====================================================
CREATE OR REPLACE FUNCTION update_webhook_events_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER webhook_events_updated_at_trigger
    BEFORE UPDATE ON webhook_events
    FOR EACH ROW
    EXECUTE FUNCTION update_webhook_events_updated_at();

CREATE OR REPLACE FUNCTION update_webhook_logs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER webhook_logs_updated_at_trigger
    BEFORE UPDATE ON webhook_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_webhook_logs_updated_at();
