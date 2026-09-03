-- cb_employee Module Migration (PostgreSQL)
-- Version: 1.0.128
-- Description: maintenance-org roster (employee)
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE employee (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    employee_no VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(40) NOT NULL DEFAULT 'EXECUTIVE',
    email VARCHAR(200),
    mobile VARCHAR(30),
    reports_to_employee_id VARCHAR(200),
    zonal_office_id VARCHAR(200),
    mapped_store_ids JSONB,
    user_id VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cb_employee_owner ON employee(owner_id);
CREATE INDEX idx_cb_employee_zone ON employee(zonal_office_id);
CREATE INDEX idx_cb_employee_user ON employee(user_id);

COMMENT ON TABLE employee IS 'maintenance-org roster with reporting hierarchy';
