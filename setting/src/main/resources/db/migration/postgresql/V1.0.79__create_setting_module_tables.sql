-- Setting Module Database Migration Script (PostgreSQL)
-- Version: 1.0.79
-- Description: Create workspace-scoped store_setting table (central key-value settings registry)
-- Dependencies: V1.0.0__create_core_tables.sql

-- =====================================================
-- Store Setting Table
-- One row per (owner_id, module_code, setting_key); values stored as text and typed via value_type.
-- =====================================================
CREATE TABLE store_setting (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    module_code VARCHAR(64) NOT NULL,
    setting_key VARCHAR(128) NOT NULL,
    value TEXT,
    value_type VARCHAR(16) NOT NULL DEFAULT 'STRING',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_store_setting_owner_module_key UNIQUE (owner_id, module_code, setting_key)
);

CREATE INDEX idx_store_setting_owner ON store_setting (owner_id);
CREATE INDEX idx_store_setting_updated ON store_setting (updated_at);
