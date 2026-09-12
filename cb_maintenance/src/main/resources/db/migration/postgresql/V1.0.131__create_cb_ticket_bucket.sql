-- cb_maintenance Module Migration (PostgreSQL)
-- Version: 1.0.131
-- Description: maintenance — ticket classification catalog (Department › Category › Sub-category).
--              Workspace-scoped (owner_id / @TenantId): each workspace owns its taxonomy, seeded
--              per workspace (see cb_maintenance seed bundle), so this migration is DDL-only.
-- Dependencies: V1.0.130 (cb_maintenance base tables)

CREATE TABLE ticket_bucket (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    department VARCHAR(100) NOT NULL,
    category VARCHAR(150) NOT NULL,
    sub_category_1 VARCHAR(200) NOT NULL,
    sub_category_2 VARCHAR(200) NOT NULL DEFAULT '',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_cb_ticket_bucket_owner ON ticket_bucket(owner_id);
CREATE INDEX idx_cb_ticket_bucket_dept ON ticket_bucket(department);
CREATE UNIQUE INDEX uk_cb_ticket_bucket
    ON ticket_bucket(owner_id, department, category, sub_category_1, sub_category_2);
