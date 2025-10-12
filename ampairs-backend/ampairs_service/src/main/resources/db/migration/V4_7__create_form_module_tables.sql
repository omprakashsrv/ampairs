-- Form Module Database Migration Script
-- Version: 4.7
-- Description: Create dynamic attribute and field configuration tables
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_1__create_core_tables.sql

-- =====================================================
-- Attribute Definition
-- =====================================================
CREATE TABLE attribute_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
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
    validation_params JSON,
    enum_values JSON,
    placeholder VARCHAR(255),
    help_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_attribute_definition (entity_type, attribute_key),
    INDEX idx_attribute_definition_owner_entity (owner_id, entity_type),
    UNIQUE INDEX idx_attribute_definition_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace custom attribute definitions for entity extensions';

-- =====================================================
-- Field Configuration
-- =====================================================
CREATE TABLE field_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
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
    validation_params JSON,
    placeholder VARCHAR(255),
    help_text TEXT,
    default_value VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_field_config (entity_type, field_name),
    INDEX idx_field_config_owner_entity (owner_id, entity_type),
    UNIQUE INDEX idx_field_config_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace field configuration metadata controlling UI behaviour';
