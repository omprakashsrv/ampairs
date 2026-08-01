-- Connector Module Database Migration (PostgreSQL)
-- Version: 1.0.126
-- Description: Per-workspace connector installations for the Apps & Extensions platform (spec 013).
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE connector_installation (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    connector_type VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'NEEDS_CONFIG',
    auto_start BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_seconds INTEGER,
    last_error_message TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_connector_installation_owner ON connector_installation (owner_id);
CREATE INDEX idx_connector_installation_owner_type ON connector_installation (owner_id, connector_type);
CREATE INDEX idx_connector_installation_updated ON connector_installation (updated_at);

-- One active install per (workspace, connector_type) unless the connector allows multiple instances.
-- Enforced as a partial unique index on active rows (service layer also guards this, FR-005).
CREATE UNIQUE INDEX uq_connector_installation_owner_type_active
    ON connector_installation (owner_id, connector_type)
    WHERE active = TRUE;
