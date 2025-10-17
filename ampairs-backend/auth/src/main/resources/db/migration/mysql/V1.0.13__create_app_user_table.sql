-- Auth Module Database Migration Script
-- Version: 4.8.1
-- Description: Create app_user table
-- Author: Codex CLI
-- Date: 2025-01-14
-- Dependencies: V1.0.1__create_auth_module_tables.sql

-- =====================================================
-- App User Table
-- =====================================================
CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    country_code INT NOT NULL DEFAULT 91,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(320),
    user_name VARCHAR(200) NOT NULL,
    user_password VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_app_user_uid (uid),
    UNIQUE INDEX uk_app_user_user_name (user_name),
    INDEX idx_app_user_phone (country_code, phone),
    INDEX idx_app_user_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Platform users managed by auth module';
