-- cb_maintenance Module Migration (MySQL)
-- Version: 1.0.130
-- Description: maintenance — PM schedules/entries, tickets, asset-category aliases
-- Dependencies: V1.0.128 (cb_employee), V1.0.129 (cb_store)

CREATE TABLE pm_schedule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    asset_category VARCHAR(100) NOT NULL,
    task_name VARCHAR(200) NOT NULL,
    checklist JSON,
    frequency_unit VARCHAR(20) NOT NULL DEFAULT 'MONTH',
    frequency_interval INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_cb_pm_schedule_uid (uid),
    INDEX idx_cb_pm_schedule_owner (owner_id),
    INDEX idx_cb_pm_schedule_category (asset_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pm_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    store_id VARCHAR(200) NOT NULL,
    zonal_office_id VARCHAR(200) NOT NULL,
    asset_category VARCHAR(100) NOT NULL,
    pm_schedule_id VARCHAR(200),
    source VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    due_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DUE',
    assigned_to_employee_id VARCHAR(200),
    assisted_by_employee_ids JSON,
    completed_at TIMESTAMP NULL,
    completed_by_employee_id VARCHAR(200),
    checklist_result JSON,
    ticket_id VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_cb_pm_entry_uid (uid),
    INDEX idx_cb_pm_entry_owner (owner_id),
    INDEX idx_cb_pm_entry_scope (zonal_office_id, store_id, status, due_date),
    INDEX idx_cb_pm_entry_cursor (store_id, pm_schedule_id, due_date),
    INDEX idx_cb_pm_entry_assignee (assigned_to_employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ticket (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    store_id VARCHAR(200) NOT NULL,
    zonal_office_id VARCHAR(200) NOT NULL,
    asset_category VARCHAR(100) NOT NULL,
    sub_category VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_to_employee_id VARCHAR(200),
    assisted_by_employee_ids JSON,
    raised_by_employee_id VARCHAR(200),
    raised_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    origin_pm_entry_id VARCHAR(200),
    suggested_spare_part VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_cb_ticket_uid (uid),
    INDEX idx_cb_ticket_owner (owner_id),
    INDEX idx_cb_ticket_scope (zonal_office_id, store_id, status, raised_at),
    INDEX idx_cb_ticket_assignee (assigned_to_employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE asset_category_alias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    canonical VARCHAR(100) NOT NULL,
    alias VARCHAR(100) NOT NULL,
    alias_lower VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_cb_asset_alias_uid (uid),
    UNIQUE INDEX uk_cb_asset_alias_owner_alias (owner_id, alias_lower),
    INDEX idx_cb_asset_alias_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
