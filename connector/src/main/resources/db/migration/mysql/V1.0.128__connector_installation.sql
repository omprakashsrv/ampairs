-- Connector Module Database Migration (MySQL)
-- Version: 1.0.93
-- Description: Per-workspace connector installations for the Apps & Extensions platform (spec 013).
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE connector_installation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    connector_type VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'NEEDS_CONFIG',
    auto_start BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_seconds INT,
    last_error_message TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_connector_installation_owner ON connector_installation (owner_id);
CREATE INDEX idx_connector_installation_owner_type ON connector_installation (owner_id, connector_type);
CREATE INDEX idx_connector_installation_updated ON connector_installation (updated_at);

-- MySQL has no partial indexes; single-active-install per (workspace, connector_type) is enforced in
-- the service layer (ConnectorInstallationService.install, FR-005).
