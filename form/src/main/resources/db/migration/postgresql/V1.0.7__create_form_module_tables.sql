-- Form Module Database Migration Script (PostgreSQL)
-- Version: 4.7
-- Description: Create dynamic attribute and field configuration tables
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.0__create_core_tables.sql

-- =====================================================
-- Attribute Definition
-- =====================================================
CREATE TABLE attribute_definition (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    attribute_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    data_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    category VARCHAR(100),
    default_value VARCHAR(255),
    validation_type VARCHAR(50),
    validation_params JSONB,
    enum_values JSONB,
    placeholder VARCHAR(255),
    help_text TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_attribute_definition ON attribute_definition(entity_type, attribute_key);
CREATE INDEX idx_attribute_definition_owner_entity ON attribute_definition(owner_id, entity_type);

COMMENT ON TABLE attribute_definition IS 'Workspace custom attribute definitions for entity extensions';

-- =====================================================
-- Field Configuration
-- =====================================================
CREATE TABLE field_config (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    validation_type VARCHAR(50),
    validation_params JSONB,
    placeholder VARCHAR(255),
    help_text TEXT,
    default_value VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_field_config ON field_config(entity_type, field_name);
CREATE INDEX idx_field_config_owner_entity ON field_config(owner_id, entity_type);

COMMENT ON TABLE field_config IS 'Workspace field configuration metadata controlling UI behaviour';
