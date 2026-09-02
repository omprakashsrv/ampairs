-- cb_employee Module Migration (MySQL)
-- Version: 1.0.128
-- Description: California Burrito maintenance-org roster (employee)
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE employee (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    employee_no VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(40) NOT NULL DEFAULT 'EXECUTIVE',
    email VARCHAR(200),
    mobile VARCHAR(30),
    reports_to_employee_id VARCHAR(200),
    zonal_office_id VARCHAR(200),
    mapped_store_ids JSON,
    user_id VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_cb_employee_uid (uid),
    INDEX idx_cb_employee_owner (owner_id),
    INDEX idx_cb_employee_zone (zonal_office_id),
    INDEX idx_cb_employee_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='California Burrito maintenance-org roster with reporting hierarchy';
