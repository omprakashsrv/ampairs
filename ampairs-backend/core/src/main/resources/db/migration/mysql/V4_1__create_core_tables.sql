-- Core Module Database Migration Script
-- Version: 4.1
-- Description: Create core storage metadata tables required by downstream modules
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V3_2__create_event_system_tables.sql

-- =====================================================
-- File Storage Metadata Table
-- =====================================================
CREATE TABLE file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    bucket VARCHAR(255) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    size BIGINT,
    etag VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_file_uid (uid),
    INDEX idx_file_owner (owner_id),
    INDEX idx_file_bucket (bucket)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores object storage metadata for workspace scoped files';
