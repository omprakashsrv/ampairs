-- Setting Module Database Migration Script
-- Version: 1.0.79
-- Description: Create workspace-scoped store_setting table (central key-value settings registry)
-- Dependencies: V1.0.0__create_core_tables.sql

-- =====================================================
-- Store Setting Table
-- One row per (owner_id, module_code, setting_key); values stored as text and typed via value_type.
-- =====================================================
CREATE TABLE store_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    module_code VARCHAR(64) NOT NULL,
    setting_key VARCHAR(128) NOT NULL,
    value TEXT,
    value_type VARCHAR(16) NOT NULL DEFAULT 'STRING',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_store_setting_uid (uid),
    UNIQUE INDEX idx_store_setting_owner_module_key (owner_id, module_code, setting_key),
    INDEX idx_store_setting_owner (owner_id),
    INDEX idx_store_setting_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-scoped module settings (central key-value registry)';
