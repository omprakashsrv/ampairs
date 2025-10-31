-- Unit Module Database Migration Script (PostgreSQL)
-- Version: 4.2
-- Description: Create measurement unit tables with conversion mappings
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.0__create_core_tables.sql

-- =====================================================
-- Unit Table
-- =====================================================
CREATE TABLE unit (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(10) NOT NULL,
    short_name VARCHAR(10) NOT NULL,
    decimal_places INT NOT NULL DEFAULT 2,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX unit_idx ON unit(name);
CREATE INDEX idx_unit_owner ON unit(owner_id);

COMMENT ON TABLE unit IS 'Workspace-scoped measurement units';

-- =====================================================
-- Unit Conversion Table
-- =====================================================
CREATE TABLE unit_conversion (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    base_unit_id VARCHAR(200) NOT NULL,
    derived_unit_id VARCHAR(200) NOT NULL,
    product_id VARCHAR(200),
    multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    CONSTRAINT fk_unit_conversion_base FOREIGN KEY (base_unit_id) REFERENCES unit (uid) ON DELETE CASCADE,
    CONSTRAINT fk_unit_conversion_derived FOREIGN KEY (derived_unit_id) REFERENCES unit (uid) ON DELETE CASCADE
);

CREATE INDEX idx_unit_conversion_base ON unit_conversion(base_unit_id);
CREATE INDEX idx_unit_conversion_derived ON unit_conversion(derived_unit_id);
CREATE INDEX idx_unit_conversion_product ON unit_conversion(product_id);
CREATE INDEX idx_unit_conversion_owner ON unit_conversion(owner_id);

COMMENT ON TABLE unit_conversion IS 'Workspace unit conversion factors optionally linked to products';
