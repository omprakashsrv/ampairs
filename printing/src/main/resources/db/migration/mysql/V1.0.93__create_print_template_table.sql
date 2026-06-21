-- Print Template Module Migration (MySQL)
-- Description: Workspace-scoped print templates synced from the mobile printing module
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE print_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    document_type VARCHAR(50) NOT NULL,
    printer_class VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    template_json TEXT NOT NULL,
    template_version BIGINT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_print_template_uid (uid),
    INDEX idx_print_template_owner (owner_id),
    INDEX idx_print_template_doc_type (document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-scoped print templates (opaque client-rendered layout JSON)';
