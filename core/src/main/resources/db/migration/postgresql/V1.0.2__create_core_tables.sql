-- Core Module Database Migration Script (PostgreSQL)
-- Version: 4.1
-- Description: Create core storage metadata tables required by downstream modules
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.0__create_event_system_tables.sql

-- =====================================================
-- File Storage Metadata Table
-- =====================================================
CREATE TABLE file
(
    id           BIGSERIAL PRIMARY KEY,
    uid          VARCHAR(40)  NOT NULL UNIQUE,
    owner_id     VARCHAR(40)  NOT NULL,
    ref_id       VARCHAR(255),
    name         VARCHAR(255) NOT NULL,
    bucket       VARCHAR(255) NOT NULL,
    object_key   VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    size         BIGINT,
    etag         VARCHAR(100),
    created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_file_owner ON file (owner_id);
CREATE INDEX idx_file_bucket ON file (bucket);

COMMENT
ON TABLE file IS 'Stores object storage metadata for workspace scoped files';
