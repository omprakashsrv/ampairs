-- Per-workspace provider credentials (sender identity + encrypted secret) — PostgreSQL
CREATE TABLE workspace_channel_credential (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
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
    last_validated_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wcc_owner_channel_provider UNIQUE (owner_id, channel, provider)
);
CREATE INDEX idx_wcc_owner ON workspace_channel_credential(owner_id);
