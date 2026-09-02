-- cb_maintenance Module Migration (PostgreSQL)
-- Version: 1.0.130
-- Description: maintenance — PM schedules/entries, tickets, asset-category aliases
-- Dependencies: V1.0.128 (cb_employee), V1.0.129 (cb_store)

CREATE TABLE pm_schedule (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    asset_category VARCHAR(100) NOT NULL,
    task_name VARCHAR(200) NOT NULL,
    checklist JSONB,
    frequency_unit VARCHAR(20) NOT NULL DEFAULT 'MONTH',
    frequency_interval INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_cb_pm_schedule_owner ON pm_schedule(owner_id);
CREATE INDEX idx_cb_pm_schedule_category ON pm_schedule(asset_category);

CREATE TABLE pm_entry (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    store_id VARCHAR(200) NOT NULL,
    zonal_office_id VARCHAR(200) NOT NULL,
    asset_category VARCHAR(100) NOT NULL,
    pm_schedule_id VARCHAR(200),
    source VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    due_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DUE',
    assigned_to_employee_id VARCHAR(200),
    assisted_by_employee_ids JSONB,
    completed_at TIMESTAMP(6),
    completed_by_employee_id VARCHAR(200),
    checklist_result JSONB,
    ticket_id VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_cb_pm_entry_owner ON pm_entry(owner_id);
CREATE INDEX idx_cb_pm_entry_scope ON pm_entry(zonal_office_id, store_id, status, due_date);
CREATE INDEX idx_cb_pm_entry_cursor ON pm_entry(store_id, pm_schedule_id, due_date);
CREATE INDEX idx_cb_pm_entry_assignee ON pm_entry(assigned_to_employee_id);

CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    store_id VARCHAR(200) NOT NULL,
    zonal_office_id VARCHAR(200) NOT NULL,
    asset_category VARCHAR(100) NOT NULL,
    sub_category VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_to_employee_id VARCHAR(200),
    assisted_by_employee_ids JSONB,
    raised_by_employee_id VARCHAR(200),
    raised_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP(6),
    origin_pm_entry_id VARCHAR(200),
    suggested_spare_part VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_cb_ticket_owner ON ticket(owner_id);
CREATE INDEX idx_cb_ticket_scope ON ticket(zonal_office_id, store_id, status, raised_at);
CREATE INDEX idx_cb_ticket_assignee ON ticket(assigned_to_employee_id);

CREATE TABLE asset_category_alias (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    canonical VARCHAR(100) NOT NULL,
    alias VARCHAR(100) NOT NULL,
    alias_lower VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_cb_asset_alias_owner_alias UNIQUE (owner_id, alias_lower)
);
CREATE INDEX idx_cb_asset_alias_owner ON asset_category_alias(owner_id);
