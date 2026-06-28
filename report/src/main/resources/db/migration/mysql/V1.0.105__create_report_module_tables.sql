-- Report Module Database Migration Script (MySQL)
-- Version: 1.0.105
-- Description: Create the export_template table (saved, syncable per-module custom reports)
-- Dependencies: V1.0.0__create_core_tables.sql

-- =====================================================
-- Export Template Table
-- =====================================================
CREATE TABLE export_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    module_key VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    selected_columns TEXT,
    filters TEXT,
    sort_by VARCHAR(100),
    sort_dir VARCHAR(4) NOT NULL DEFAULT 'ASC',
    default_format VARCHAR(10) NOT NULL DEFAULT 'CSV',
    default_location VARCHAR(10) NOT NULL DEFAULT 'CLIENT',
    include_inactive BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_export_template_uid (uid),
    INDEX idx_export_template_owner (owner_id),
    INDEX idx_export_template_module (module_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-scoped saved export/report templates (column-select + filters)';
