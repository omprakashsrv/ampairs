-- Connector Module Database Migration (PostgreSQL)
-- Version: 1.0.128
-- Description: Connector sync checkpoints (incremental watermarks) + run history (spec 013).
-- Dependencies: V1.0.127__connector_config_mapping.sql

CREATE TABLE connector_sync_checkpoint (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    installation_uid VARCHAR(200) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL DEFAULT 'INBOUND',
    watermark VARCHAR(255),
    last_synced_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_connector_checkpoint_key UNIQUE (installation_uid, entity_type, direction)
);

CREATE INDEX idx_connector_checkpoint_owner ON connector_sync_checkpoint (owner_id);
CREATE INDEX idx_connector_checkpoint_updated ON connector_sync_checkpoint (updated_at);

CREATE TABLE connector_sync_run (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    installation_uid VARCHAR(200) NOT NULL,
    entity_type VARCHAR(64),
    trigger_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP(6),
    status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    processed INTEGER NOT NULL DEFAULT 0,
    created INTEGER NOT NULL DEFAULT 0,
    updated INTEGER NOT NULL DEFAULT 0,
    failed INTEGER NOT NULL DEFAULT 0,
    error_detail TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_connector_run_owner ON connector_sync_run (owner_id);
CREATE INDEX idx_connector_run_installation ON connector_sync_run (installation_uid);
CREATE INDEX idx_connector_run_updated ON connector_sync_run (updated_at);
