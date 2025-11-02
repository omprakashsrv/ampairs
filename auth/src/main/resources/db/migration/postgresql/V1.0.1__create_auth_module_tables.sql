-- Auth Module Database Migration Script (PostgreSQL)
-- Version: 4.8
-- Description: Create device_session, login_session, and auth_token tables
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.0__create_form_module_tables.sql

-- =====================================================
-- Device Session Table
-- =====================================================
CREATE TABLE device_session (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    user_id VARCHAR(200) NOT NULL,
    device_id VARCHAR(200) NOT NULL,
    device_name VARCHAR(255),
    device_type VARCHAR(50),
    platform VARCHAR(50),
    browser VARCHAR(100),
    os VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    location VARCHAR(255),
    last_activity TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    refresh_token_hash VARCHAR(500),
    expired_at TIMESTAMP(6),

    CONSTRAINT uk_device_session_uid UNIQUE (uid)
);

CREATE INDEX idx_device_session_user_active ON device_session (user_id, is_active);
CREATE INDEX idx_device_session_user_device ON device_session (user_id, device_id);
CREATE INDEX idx_device_session_active_last_activity ON device_session (is_active, last_activity);
CREATE INDEX idx_device_session_refresh_token ON device_session (refresh_token_hash);

-- =====================================================
-- Login Session Table
-- =====================================================
CREATE TABLE login_session (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    country_code INT NOT NULL,
    phone VARCHAR(12) NOT NULL,
    user_agent VARCHAR(255) NOT NULL,
    expiry_time TIMESTAMP(6) NOT NULL,
    code VARCHAR(255),
    attempts INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP(6),
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',

    CONSTRAINT uk_login_session_uid UNIQUE (uid)
);

CREATE INDEX idx_login_session_phone_verified ON login_session (phone, country_code, verified);
CREATE INDEX idx_login_session_expiry ON login_session (expiry_time);
CREATE INDEX idx_login_session_created_at ON login_session (created_at);

-- =====================================================
-- Auth Token Table
-- =====================================================
CREATE TABLE auth_token (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    token VARCHAR(500) NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    user_id VARCHAR(200) NOT NULL,
    device_id VARCHAR(200),
    token_type VARCHAR(20) NOT NULL,

    CONSTRAINT uk_auth_token_token UNIQUE (token)
);

CREATE INDEX idx_auth_token_user ON auth_token (user_id);
CREATE INDEX idx_auth_token_status ON auth_token (expired, revoked);
CREATE INDEX idx_auth_token_created_at ON auth_token (created_at);
