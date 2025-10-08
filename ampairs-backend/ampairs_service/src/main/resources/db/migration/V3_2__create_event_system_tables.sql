-- Event System Database Migration Script
-- Version: 3.2
-- Description: Create event synchronization and device status tracking tables for multi-device support
-- Author: Claude Code
-- Date: 2025-01-06

-- =====================================================
-- Workspace Events Table
-- =====================================================
CREATE TABLE workspace_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(200) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(200) NOT NULL,
    payload TEXT NOT NULL,
    device_id VARCHAR(200) NOT NULL,
    user_id VARCHAR(200) NOT NULL,
    sequence_number BIGINT NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    INDEX idx_workspace_events_workspace (workspace_id),
    INDEX idx_workspace_events_sequence (workspace_id, sequence_number),
    INDEX idx_workspace_events_entity (workspace_id, entity_type, entity_id),
    INDEX idx_workspace_events_type (workspace_id, event_type),
    INDEX idx_workspace_events_consumed (consumed, created_at),
    INDEX idx_workspace_events_device (device_id),
    INDEX idx_workspace_events_user (user_id),
    INDEX idx_workspace_events_uid (uid),
    UNIQUE KEY uk_workspace_sequence (workspace_id, sequence_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Event store for workspace entity changes - supports offline sync and multi-device collaboration';

-- =====================================================
-- Device Sessions Table
-- =====================================================
CREATE TABLE device_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(200) NOT NULL,
    user_id VARCHAR(200) NOT NULL,
    device_id VARCHAR(200) NOT NULL,
    device_name VARCHAR(255),
    session_id VARCHAR(200) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    last_heartbeat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    INDEX idx_device_sessions_workspace (workspace_id),
    INDEX idx_device_sessions_user (workspace_id, user_id),
    INDEX idx_device_sessions_device (workspace_id, user_id, device_id),
    INDEX idx_device_sessions_status (status, last_heartbeat),
    INDEX idx_device_sessions_session (session_id),
    INDEX idx_device_sessions_heartbeat (last_heartbeat),
    INDEX idx_device_sessions_uid (uid),
    UNIQUE KEY uk_workspace_user_device (workspace_id, user_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks device connection status (online/away/offline) for real-time presence and multi-device sync';

-- =====================================================
-- Initial Sequence Numbers
-- =====================================================
-- Note: Sequence numbers start at 1 for each workspace
-- Managed by WorkspaceEventRepository.getNextSequenceNumber()

-- =====================================================
-- Status Enum Values
-- =====================================================
-- workspace_events.event_type: CUSTOMER_CREATED, CUSTOMER_UPDATED, CUSTOMER_DELETED,
--                              PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED, PRODUCT_STOCK_CHANGED,
--                              ORDER_CREATED, ORDER_UPDATED, ORDER_STATUS_CHANGED, ORDER_DELETED,
--                              INVOICE_CREATED, INVOICE_UPDATED, INVOICE_PAID, INVOICE_DELETED,
--                              USER_STATUS_CHANGED, DEVICE_CONNECTED, DEVICE_DISCONNECTED
--
-- device_sessions.status: ONLINE (active with recent heartbeat),
--                        AWAY (no activity 30s-2min),
--                        OFFLINE (>2min no heartbeat or disconnected)
