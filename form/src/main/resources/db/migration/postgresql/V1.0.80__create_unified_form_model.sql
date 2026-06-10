-- Unified Form Model (PostgreSQL)
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
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_form_schema_owner_entity ON form_schema(owner_id, entity_type);
CREATE INDEX idx_form_schema_owner_entity ON form_schema(owner_id, entity_type);

COMMENT ON TABLE form_schema IS 'Per-workspace form schema aggregate header (one per entity_type); version = optimistic-concurrency stamp';

-- =====================================================
-- Form Section (aggregate member)
-- =====================================================
CREATE TABLE form_section (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_form_section_owner_entity_order ON form_section(owner_id, entity_type, display_order);

COMMENT ON TABLE form_section IS 'Configurable section grouping fields within one entity-type form (no soft-delete; delete-by-absence)';

-- =====================================================
-- Form Field (aggregate member; unified standard + custom)
-- =====================================================
CREATE TABLE form_field (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
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
    enum_values JSONB,
    dynamic_source_key VARCHAR(100),
    validation_rules JSONB,
    placeholder VARCHAR(255),
    help_text TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_form_field_owner_entity_source_key ON form_field(owner_id, entity_type, source, field_key);
CREATE INDEX idx_form_field_owner_entity ON form_field(owner_id, entity_type);
CREATE INDEX idx_form_field_section_order ON form_field(entity_type, section_uid, display_order);

COMMENT ON TABLE form_field IS 'Unified form field (standard | custom) belonging to one section; no soft-delete (delete-by-absence)';
