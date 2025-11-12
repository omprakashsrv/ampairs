-- Create api_keys table for machine-to-machine authentication
-- Used by CI/CD pipelines and automated systems

CREATE TABLE api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,

    -- Key information
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    key_prefix VARCHAR(20) NOT NULL,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP NULL,

    -- Usage tracking
    last_used_at TIMESTAMP NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,

    -- Permissions
    scope VARCHAR(50) NOT NULL,

    -- Audit fields
    created_by_user_id VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),

    -- Revocation
    revoked_at TIMESTAMP NULL,
    revoked_by VARCHAR(100),
    revoked_reason VARCHAR(500),

    -- Constraints
    CONSTRAINT chk_api_keys_scope CHECK (scope IN ('APP_UPDATES', 'READ_ONLY', 'FULL_ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for fast lookup
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_active ON api_keys(is_active, expires_at);

-- Comments
ALTER TABLE api_keys COMMENT = 'API keys for machine-to-machine authentication (CI/CD, integrations)';
