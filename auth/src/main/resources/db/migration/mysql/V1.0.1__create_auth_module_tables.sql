-- Auth Module Database Migration Script
-- Version: 4.8
-- Description: Create device_session, login_session, and auth_token tables
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.0__create_form_module_tables.sql

-- =====================================================
-- Device Session Table
-- =====================================================
CREATE TABLE device_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


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
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    refresh_token_hash VARCHAR(500),
    expired_at TIMESTAMP NULL,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_device_session_uid (uid),
    INDEX idx_device_session_user_active (user_id, is_active),
    INDEX idx_device_session_user_device (user_id, device_id),
    INDEX idx_device_session_active_last_activity (is_active, last_activity),
    INDEX idx_device_session_refresh_token (refresh_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks authenticated device sessions per user';

-- =====================================================
-- Login Session Table
-- =====================================================
CREATE TABLE login_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    country_code INT NOT NULL,
    phone VARCHAR(12) NOT NULL,
    user_agent VARCHAR(255) NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    code VARCHAR(10),
    attempts INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP NULL,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',

    PRIMARY KEY (id),
    UNIQUE INDEX idx_login_session_uid (uid),
    INDEX idx_login_session_phone_verified (phone, country_code, verified),
    INDEX idx_login_session_expiry (expiry_time),
    INDEX idx_login_session_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='OTP login sessions and verification state';

-- =====================================================
-- Auth Token Table
-- =====================================================
CREATE TABLE auth_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    token VARCHAR(500) NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    user_id VARCHAR(200) NOT NULL,
    device_id VARCHAR(200),
    token_type VARCHAR(20) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_auth_token_uid (uid),
    UNIQUE INDEX uk_auth_token_token (token),
    INDEX idx_auth_token_user (user_id),
    INDEX idx_auth_token_status (expired, revoked),
    INDEX idx_auth_token_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Issued access and refresh tokens with revocation flags';
