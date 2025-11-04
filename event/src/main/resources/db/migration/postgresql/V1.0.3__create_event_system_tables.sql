-- Event System Database Migration Script (PostgreSQL)
-- Version: 3.2
-- Description: Create event synchronization and device status tracking tables for multi-device support
-- Author: Codex CLI
-- Date: 2025-01-06

-- =====================================================
-- Workspace Events Table
-- =====================================================
CREATE TABLE workspace_events
(
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(40)  NOT NULL UNIQUE,
    workspace_id    VARCHAR(40)  NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       VARCHAR(200) NOT NULL,
    payload         TEXT         NOT NULL,
    device_id       VARCHAR(200) NOT NULL,
    user_id         VARCHAR(200) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    consumed        BOOLEAN      NOT NULL DEFAULT FALSE,
    owner_id        VARCHAR(200) NOT NULL,
    ref_id          VARCHAR(255),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated    BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_workspace_events_workspace ON workspace_events (workspace_id);
CREATE INDEX idx_workspace_events_sequence ON workspace_events (workspace_id, sequence_number);
CREATE INDEX idx_workspace_events_entity ON workspace_events (workspace_id, entity_type, entity_id);
CREATE INDEX idx_workspace_events_type ON workspace_events (workspace_id, event_type);
CREATE INDEX idx_workspace_events_consumed ON workspace_events (consumed, created_at);
CREATE INDEX idx_workspace_events_device ON workspace_events (device_id);
CREATE INDEX idx_workspace_events_user ON workspace_events (user_id);

CREATE UNIQUE INDEX uk_workspace_sequence ON workspace_events (workspace_id, sequence_number);

COMMENT
ON TABLE workspace_events IS 'Event store for workspace entity changes - supports offline sync and multi-device collaboration';

-- =====================================================
-- Device Sessions Table
-- =====================================================
CREATE TABLE device_sessions
(
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(40)  NOT NULL UNIQUE,
    workspace_id    VARCHAR(40)  NOT NULL,
    user_id         VARCHAR(40)  NOT NULL,
    device_id       VARCHAR(200) NOT NULL,
    device_name     VARCHAR(255),
    session_id      VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ONLINE',
    last_heartbeat  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    connected_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP(6),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated    BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_device_sessions_workspace ON device_sessions (workspace_id);
CREATE INDEX idx_device_sessions_user ON device_sessions (workspace_id, user_id);
CREATE INDEX idx_device_sessions_device ON device_sessions (workspace_id, user_id, device_id);
CREATE INDEX idx_device_sessions_status ON device_sessions (status, last_heartbeat);
CREATE INDEX idx_device_sessions_session ON device_sessions (session_id);
CREATE INDEX idx_device_sessions_heartbeat ON device_sessions (last_heartbeat);

CREATE UNIQUE INDEX uk_device_sessions_session ON device_sessions (session_id);
CREATE UNIQUE INDEX uk_workspace_user_device ON device_sessions (workspace_id, user_id, device_id);

COMMENT
ON TABLE device_sessions IS 'Tracks device connection status (online/away/offline) for real-time presence and multi-device sync';
