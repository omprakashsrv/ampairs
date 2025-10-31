-- Auth Module Database Migration Script (PostgreSQL)
-- Version: 4.8.1
-- Description: Create app_user table
-- Author: Codex CLI
-- Date: 2025-01-14
-- Dependencies: V1.0.1__create_auth_module_tables.sql

-- =====================================================
-- App User Table
-- =====================================================
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    country_code INT NOT NULL DEFAULT 91,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(320),
    user_name VARCHAR(200) NOT NULL,
    user_password VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uk_app_user_user_name ON app_user (user_name);
CREATE INDEX idx_app_user_phone ON app_user (country_code, phone);
CREATE INDEX idx_app_user_active ON app_user (active);
