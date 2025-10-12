-- Unit Module Database Migration Script
-- Version: 4.2
-- Description: Create measurement unit tables with conversion mappings
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_1__create_core_tables.sql

-- =====================================================
-- Unit Table
-- =====================================================
CREATE TABLE unit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(10) NOT NULL,
    short_name VARCHAR(10) NOT NULL,
    decimal_places INT NOT NULL DEFAULT 2,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_unit_uid (uid),
    INDEX unit_idx (name),
    INDEX idx_unit_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-scoped measurement units';

-- =====================================================
-- Unit Conversion Table
-- =====================================================
CREATE TABLE unit_conversion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    base_unit_id VARCHAR(200) NOT NULL,
    derived_unit_id VARCHAR(200) NOT NULL,
    product_id VARCHAR(200),
    multiplier DOUBLE NOT NULL DEFAULT 1.0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_unit_conversion_uid (uid),
    INDEX idx_unit_conversion_base (base_unit_id),
    INDEX idx_unit_conversion_derived (derived_unit_id),
    INDEX idx_unit_conversion_product (product_id),
    INDEX idx_unit_conversion_owner (owner_id),

    CONSTRAINT fk_unit_conversion_base FOREIGN KEY (base_unit_id) REFERENCES unit (uid) ON DELETE CASCADE,
    CONSTRAINT fk_unit_conversion_derived FOREIGN KEY (derived_unit_id) REFERENCES unit (uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace unit conversion factors optionally linked to products';
