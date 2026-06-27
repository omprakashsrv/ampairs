-- Communication Module Migration (PostgreSQL)
-- Description: Generic communication orchestration — templates, requests, logs, schedules,
--              campaigns, consent, event bindings, usage ledger. Workspace-scoped (owner_id).
-- Dependencies: V1.0.0__create_core_tables.sql

-- 1. message_template (aggregate root)
CREATE TABLE message_template (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    code VARCHAR(120) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(20) NOT NULL,
    default_locale VARCHAR(16) NOT NULL DEFAULT 'en',
    description TEXT,
    base_version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_message_template_owner_code UNIQUE (owner_id, code)
);
CREATE INDEX idx_message_template_owner ON message_template(owner_id);

-- 2. message_template_variant (child of template)
CREATE TABLE message_template_variant (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    template_uid VARCHAR(200) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    locale VARCHAR(16) NOT NULL DEFAULT 'en',
    subject VARCHAR(500),
    html_body TEXT,
    text_body TEXT,
    provider_template_id VARCHAR(200),
    provider_params_json TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_variant_template_channel_locale UNIQUE (template_uid, channel, locale)
);
CREATE INDEX idx_variant_template ON message_template_variant(template_uid);
CREATE INDEX idx_variant_owner ON message_template_variant(owner_id);

-- 3. communication_request (one logical send)
CREATE TABLE communication_request (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    template_uid VARCHAR(200),
    trigger_type VARCHAR(20) NOT NULL,
    source_ref VARCHAR(200),
    channels VARCHAR(120) NOT NULL,
    audience_type VARCHAR(20) NOT NULL,
    audience_ref VARCHAR(200),
    variables_json TEXT,
    dedup_key VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_request_owner_dedup UNIQUE (owner_id, dedup_key)
);
CREATE INDEX idx_request_owner ON communication_request(owner_id);

-- 4. communication_log (per recipient x channel delivery)
CREATE TABLE communication_log (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    request_uid VARCHAR(200) NOT NULL,
    customer_uid VARCHAR(200),
    channel VARCHAR(20) NOT NULL,
    recipient_address VARCHAR(320) NOT NULL,
    category VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    skip_reason VARCHAR(30),
    notification_uid VARCHAR(200),
    provider_message_id VARCHAR(255),
    error_message TEXT,
    occurrence_key VARCHAR(64),
    credential_uid VARCHAR(200),
    provider_account_ref VARCHAR(200),
    billing_mode VARCHAR(20),
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_log_owner ON communication_log(owner_id);
CREATE INDEX idx_log_request ON communication_log(request_uid);
CREATE INDEX idx_log_notification ON communication_log(notification_uid);
CREATE INDEX idx_log_customer_channel ON communication_log(customer_uid, channel);

-- 5. communication_schedule
CREATE TABLE communication_schedule (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(200) NOT NULL,
    template_uid VARCHAR(200) NOT NULL,
    channels VARCHAR(120) NOT NULL,
    audience_type VARCHAR(20) NOT NULL,
    audience_ref VARCHAR(200),
    variables_json TEXT,
    frequency VARCHAR(20) NOT NULL,
    interval_count INT NOT NULL DEFAULT 1,
    day_of_week INT,
    day_of_month INT,
    time_of_day VARCHAR(5) NOT NULL,
    start_date VARCHAR(10),
    end_date VARCHAR(10),
    paused BOOLEAN NOT NULL DEFAULT FALSE,
    next_run_at TIMESTAMPTZ,
    last_run_at TIMESTAMPTZ,
    last_occurrence_key VARCHAR(64),
    claim_version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_schedule_owner ON communication_schedule(owner_id);
CREATE INDEX idx_schedule_due ON communication_schedule(paused, next_run_at);

-- 6. communication_occurrence (at-most-once ledger; server-internal)
CREATE TABLE communication_occurrence (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    schedule_uid VARCHAR(200) NOT NULL,
    occurrence_key VARCHAR(64) NOT NULL,
    materialized_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_occurrence_schedule_key UNIQUE (schedule_uid, occurrence_key)
);

-- 7. campaign
CREATE TABLE campaign (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(200) NOT NULL,
    template_uid VARCHAR(200) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    audience_type VARCHAR(20) NOT NULL,
    audience_ref VARCHAR(200),
    variables_json TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    scheduled_at TIMESTAMPTZ,
    throttle_per_minute INT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    targeted_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_campaign_owner ON campaign(owner_id);

-- 8. communication_preference (consent)
CREATE TABLE communication_preference (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    customer_uid VARCHAR(200) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    opted_in BOOLEAN NOT NULL DEFAULT TRUE,
    source VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pref_owner_customer_channel_category UNIQUE (owner_id, customer_uid, channel, category)
);
CREATE INDEX idx_pref_owner ON communication_preference(owner_id);

-- 9. communication_suppression (address block list)
CREATE TABLE communication_suppression (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    channel VARCHAR(20) NOT NULL,
    address VARCHAR(320) NOT NULL,
    reason VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_suppression_owner_channel_address UNIQUE (owner_id, channel, address)
);

-- 10. communication_config (one row per workspace)
CREATE TABLE communication_config (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    quiet_hours_start VARCHAR(5),
    quiet_hours_end VARCHAR(5),
    default_throttle_per_minute INT NOT NULL DEFAULT 60,
    promotional_footer_html TEXT,
    unsubscribe_base_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_config_owner UNIQUE (owner_id)
);

-- 11. event_template_binding (transactional trigger map)
CREATE TABLE event_template_binding (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    event_type VARCHAR(80) NOT NULL,
    template_uid VARCHAR(200) NOT NULL,
    channels VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_binding_owner_event UNIQUE (owner_id, event_type)
);

-- 12. communication_usage (append-only billing ledger)
CREATE TABLE communication_usage (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    communication_log_uid VARCHAR(200) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    credential_uid VARCHAR(200),
    provider_account_ref VARCHAR(200),
    billing_mode VARCHAR(20) NOT NULL,
    provider_message_id VARCHAR(255),
    cost_units INT NOT NULL DEFAULT 1,
    cost_category VARCHAR(40),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usage_log UNIQUE (communication_log_uid)
);
CREATE INDEX idx_usage_owner ON communication_usage(owner_id);
