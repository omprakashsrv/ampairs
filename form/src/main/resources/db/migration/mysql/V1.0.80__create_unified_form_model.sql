-- Unified Form Model (MySQL)
-- Replaces the split field_config / attribute_definition tables with a single FormSchema aggregate:
-- form_schema (aggregate header + version) -> form_section + form_field (members).
-- Fresh setup: no backfill. Deletions propagate by absence (no soft-delete column).
-- NOTE: confirm the final version with `./gradlew :ampairs_service:flywayInfo` before deploy.

DROP TABLE IF EXISTS field_config;
DROP TABLE IF EXISTS attribute_definition;

-- =====================================================
-- Form Schema (aggregate root header)
-- =====================================================
CREATE TABLE form_schema (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_form_schema_uid (uid),
    UNIQUE INDEX uk_form_schema_owner_entity (owner_id, entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Per-workspace form schema aggregate header (one per entity_type); version = optimistic stamp';

-- =====================================================
-- Form Section (aggregate member)
-- =====================================================
CREATE TABLE form_section (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_form_section_uid (uid),
    INDEX idx_form_section_owner_entity_order (owner_id, entity_type, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Configurable section grouping fields within one entity-type form (delete-by-absence)';

-- =====================================================
-- Form Field (aggregate member; unified standard + custom)
-- =====================================================
CREATE TABLE form_field (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'custom',
    field_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    data_type VARCHAR(20) NOT NULL DEFAULT 'text',
    widget_key VARCHAR(100),
    section_uid VARCHAR(200) NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    default_value VARCHAR(255),
    option_source VARCHAR(20),
    enum_values JSON,
    dynamic_source_key VARCHAR(100),
    validation_rules JSON,
    placeholder VARCHAR(255),
    help_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_form_field_uid (uid),
    UNIQUE INDEX uk_form_field_owner_entity_source_key (owner_id, entity_type, source, field_key),
    INDEX idx_form_field_owner_entity (owner_id, entity_type),
    INDEX idx_form_field_section_order (entity_type, section_uid, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Unified form field (standard | custom) belonging to one section; delete-by-absence';
