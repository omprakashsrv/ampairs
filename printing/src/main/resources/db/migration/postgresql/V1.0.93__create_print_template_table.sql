-- Print Template Module Migration (PostgreSQL)
-- Description: Workspace-scoped print templates synced from the mobile printing module
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE print_template (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    document_type VARCHAR(50) NOT NULL,
    printer_class VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    template_json TEXT NOT NULL,
    template_version BIGINT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_print_template_owner ON print_template(owner_id);
CREATE INDEX idx_print_template_doc_type ON print_template(document_type);

COMMENT ON TABLE print_template IS 'Workspace-scoped print templates (opaque client-rendered layout JSON)';
