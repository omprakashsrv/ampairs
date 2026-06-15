-- Connector Module Database Migration (MySQL)
-- Version: 1.0.94
-- Description: Connector connection config (encrypted secrets) + per-entity field mappings (spec 013).
-- Dependencies: V1.0.93__connector_installation.sql

CREATE TABLE connector_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    installation_uid VARCHAR(200) NOT NULL,
    non_secret_values TEXT,
    secret_values_encrypted TEXT,
    last_validated_at TIMESTAMP(6) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_connector_config_installation UNIQUE (installation_uid)
);

CREATE INDEX idx_connector_config_owner ON connector_config (owner_id);
CREATE INDEX idx_connector_config_updated ON connector_config (updated_at);

CREATE TABLE connector_field_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    installation_uid VARCHAR(200) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    rules TEXT,
    version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_connector_mapping_install_entity UNIQUE (installation_uid, entity_type)
);

CREATE INDEX idx_connector_mapping_owner ON connector_field_mapping (owner_id);
CREATE INDEX idx_connector_mapping_updated ON connector_field_mapping (updated_at);
