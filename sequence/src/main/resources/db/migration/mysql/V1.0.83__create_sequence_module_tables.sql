-- Sequence Module Database Migration Script (MySQL)
-- Version: 1.0.83
-- Description: Create workspace-scoped sequence_definition and sequence_allocation tables
-- Dependencies: V1.0.0__create_core_tables.sql

-- =====================================================
-- Sequence Definition Table
-- One configurable numbering scheme per (owner_id, entity_type, scope, user_id).
-- current_value is the monotonic high-water mark (last issued/allocated value).
-- At most one active row per key — enforced at the service layer (no partial
-- unique indexes in MySQL; inactive history rows are kept).
-- =====================================================
CREATE TABLE sequence_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    entity_type VARCHAR(64) NOT NULL,
    scope VARCHAR(16) NOT NULL DEFAULT 'WORKSPACE',
    user_id VARCHAR(200),
    prefix VARCHAR(32),
    suffix VARCHAR(32),
    padding_length INT NOT NULL DEFAULT 0,
    start_value BIGINT NOT NULL DEFAULT 1,
    increment_step INT NOT NULL DEFAULT 1,
    current_value BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_sequence_definition_uid (uid),
    INDEX idx_sequence_definition_owner_entity (owner_id, entity_type),
    INDEX idx_sequence_definition_owner_updated (owner_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-scoped sequence numbering schemes';

-- =====================================================
-- Sequence Allocation Table
-- Exclusive contiguous number blocks granted to devices. Ranges for one definition
-- never overlap; abandoned remainders are never reissued (gaps OK, duplicates never).
-- prefix/suffix/padding_length/increment_step snapshot the definition format at grant
-- time so devices format offline without the definition row.
-- =====================================================
CREATE TABLE sequence_allocation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    definition_uid VARCHAR(200) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    device_id VARCHAR(200) NOT NULL,
    user_id VARCHAR(200),
    range_start BIGINT NOT NULL,
    range_end BIGINT NOT NULL,
    next_available BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    prefix VARCHAR(32),
    suffix VARCHAR(32),
    padding_length INT NOT NULL DEFAULT 0,
    increment_step INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_sequence_allocation_uid (uid),
    INDEX idx_sequence_allocation_owner_device (owner_id, device_id, status),
    INDEX idx_sequence_allocation_owner_definition (owner_id, definition_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Device-scoped sequence number block allocations';
