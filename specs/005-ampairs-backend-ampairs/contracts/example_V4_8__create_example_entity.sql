-- Example Module Database Migration Script
-- Version: 4.8
-- Description: Demonstrates common patterns for Flyway migrations
-- Author: Template Bot
-- Date: 2025-10-12
-- Dependencies: V4_7__create_form_module_tables.sql

-- =====================================================
-- Example Parent Table
-- =====================================================
CREATE TABLE example_parent (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    attributes JSON NOT NULL,
    location POINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_example_parent_uid (uid),
    INDEX idx_example_parent_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Example Child Table
-- =====================================================
CREATE TABLE example_child (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    parent_uid VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    metadata JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_example_child_uid (uid),
    INDEX idx_example_child_parent (parent_uid),

    CONSTRAINT fk_example_child_parent FOREIGN KEY (parent_uid) REFERENCES example_parent (uid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
