-- Communication Module Migration (MySQL)
-- Description: Generic communication orchestration — templates, requests, logs, schedules,
--              campaigns, consent, event bindings, usage ledger. Workspace-scoped (owner_id).
-- Dependencies: V1.0.0__create_core_tables.sql

-- 1. message_template (aggregate root)
CREATE TABLE message_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    code VARCHAR(120) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(20) NOT NULL,
    default_locale VARCHAR(16) NOT NULL DEFAULT 'en',
    description TEXT,
    base_version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_message_template_uid (uid),
    UNIQUE INDEX uq_message_template_owner_code (owner_id, code),
    INDEX idx_message_template_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. message_template_variant
CREATE TABLE message_template_variant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_variant_uid (uid),
    UNIQUE INDEX uq_variant_template_channel_locale (template_uid, channel, locale),
    INDEX idx_variant_template (template_uid),
    INDEX idx_variant_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. communication_request
CREATE TABLE communication_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_request_uid (uid),
    UNIQUE INDEX uq_request_owner_dedup (owner_id, dedup_key),
    INDEX idx_request_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. communication_log
CREATE TABLE communication_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
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
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_log_uid (uid),
    INDEX idx_log_owner (owner_id),
    INDEX idx_log_request (request_uid),
    INDEX idx_log_notification (notification_uid),
    INDEX idx_log_customer_channel (customer_uid, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. communication_schedule
CREATE TABLE communication_schedule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
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
    next_run_at TIMESTAMP NULL,
    last_run_at TIMESTAMP NULL,
    last_occurrence_key VARCHAR(64),
    claim_version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_schedule_uid (uid),
    INDEX idx_schedule_owner (owner_id),
    INDEX idx_schedule_due (paused, next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. communication_occurrence
CREATE TABLE communication_occurrence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    schedule_uid VARCHAR(200) NOT NULL,
    occurrence_key VARCHAR(64) NOT NULL,
    materialized_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_occurrence_uid (uid),
    UNIQUE INDEX uq_occurrence_schedule_key (schedule_uid, occurrence_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. campaign
CREATE TABLE campaign (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(200) NOT NULL,
    template_uid VARCHAR(200) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    audience_type VARCHAR(20) NOT NULL,
    audience_ref VARCHAR(200),
    variables_json TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    scheduled_at TIMESTAMP NULL,
    throttle_per_minute INT,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    targeted_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_campaign_uid (uid),
    INDEX idx_campaign_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. communication_preference
CREATE TABLE communication_preference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    customer_uid VARCHAR(200) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    opted_in BOOLEAN NOT NULL DEFAULT TRUE,
    source VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_pref_uid (uid),
    UNIQUE INDEX uq_pref_owner_customer_channel_category (owner_id, customer_uid, channel, category),
    INDEX idx_pref_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. communication_suppression
CREATE TABLE communication_suppression (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    channel VARCHAR(20) NOT NULL,
    address VARCHAR(320) NOT NULL,
    reason VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_suppression_uid (uid),
    UNIQUE INDEX uq_suppression_owner_channel_address (owner_id, channel, address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. communication_config
CREATE TABLE communication_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    quiet_hours_start VARCHAR(5),
    quiet_hours_end VARCHAR(5),
    default_throttle_per_minute INT NOT NULL DEFAULT 60,
    promotional_footer_html TEXT,
    unsubscribe_base_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_config_uid (uid),
    UNIQUE INDEX uq_config_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. event_template_binding
CREATE TABLE event_template_binding (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    event_type VARCHAR(80) NOT NULL,
    template_uid VARCHAR(200) NOT NULL,
    channels VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_binding_uid (uid),
    UNIQUE INDEX uq_binding_owner_event (owner_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. communication_usage
CREATE TABLE communication_usage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
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
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_usage_uid (uid),
    UNIQUE INDEX uq_usage_log (communication_log_uid),
    INDEX idx_usage_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
