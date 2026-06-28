-- Per-workspace provider credentials (sender identity + encrypted secret) — MySQL
CREATE TABLE workspace_channel_credential (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    channel VARCHAR(20) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    sender_ref VARCHAR(200) NOT NULL,
    display_name VARCHAR(200),
    secret_ciphertext TEXT,
    secret_last4 VARCHAR(8),
    config_json TEXT,
    allow_platform_fallback BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    last_validated_at TIMESTAMP NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_wcc_uid (uid),
    UNIQUE INDEX uq_wcc_owner_channel_provider (owner_id, channel, provider),
    INDEX idx_wcc_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
