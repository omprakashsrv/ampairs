-- Subscription Module Database Migration Script
-- Version: 1.0.30
-- Description: Create subscription module tables
-- Author: Claude Code
-- Date: 2025-11-23
-- Dependencies: V1.0.5__create_workspace_module_tables.sql

-- =====================
-- Subscription Plan Definition (Master Data)
-- =====================
CREATE TABLE IF NOT EXISTS subscription_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    plan_code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Pricing
    monthly_price_inr DECIMAL(10,2) NOT NULL DEFAULT 0,
    monthly_price_usd DECIMAL(10,2) NOT NULL DEFAULT 0,

    -- Limits
    max_workspaces INT NOT NULL DEFAULT 1,
    max_members_per_workspace INT NOT NULL DEFAULT 1,
    max_storage_gb INT NOT NULL DEFAULT 1,
    max_customers INT NOT NULL DEFAULT 50,
    max_products INT NOT NULL DEFAULT 50,
    max_invoices_per_month INT NOT NULL DEFAULT 20,
    max_devices INT NOT NULL DEFAULT 2,
    data_retention_years INT NOT NULL DEFAULT 1,

    -- Features
    available_modules TEXT DEFAULT '["CUSTOMER","PRODUCT","INVOICE"]',
    api_access_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    custom_branding_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sso_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    audit_logs_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    priority_support BOOLEAN NOT NULL DEFAULT FALSE,

    -- Trial
    trial_days INT NOT NULL DEFAULT 0,

    -- Provider Product IDs
    google_play_product_id_monthly VARCHAR(200),
    google_play_product_id_annual VARCHAR(200),
    app_store_product_id_monthly VARCHAR(200),
    app_store_product_id_annual VARCHAR(200),
    razorpay_plan_id_monthly VARCHAR(200),
    razorpay_plan_id_annual VARCHAR(200),
    stripe_price_id_monthly VARCHAR(200),
    stripe_price_id_annual VARCHAR(200),

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_sub_plan_uid (uid),
    INDEX idx_sub_plan_code (plan_code),
    INDEX idx_sub_plan_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Subscriptions
-- =====================
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(200) NOT NULL,
    plan_id VARCHAR(200),
    plan_code VARCHAR(50) NOT NULL DEFAULT 'FREE',

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',

    -- Payment Provider
    payment_provider VARCHAR(30),
    external_subscription_id VARCHAR(255),
    external_customer_id VARCHAR(255),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    -- Billing Period
    current_period_start TIMESTAMP NULL,
    current_period_end TIMESTAMP NULL,
    trial_ends_at TIMESTAMP NULL,

    -- Cancellation
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMP NULL,
    cancellation_reason VARCHAR(500),

    -- Pause
    paused_at TIMESTAMP NULL,
    resume_at TIMESTAMP NULL,

    -- Payment Status
    next_billing_amount DECIMAL(10,2),
    last_payment_status VARCHAR(20),
    last_payment_at TIMESTAMP NULL,
    failed_payment_count INT NOT NULL DEFAULT 0,
    grace_period_ends_at TIMESTAMP NULL,

    -- Flags
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    metadata TEXT DEFAULT '{}',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_subscription_uid (uid),
    INDEX idx_subscription_workspace (workspace_id),
    INDEX idx_subscription_status (status),
    INDEX idx_subscription_provider (payment_provider),
    INDEX idx_subscription_period_end (current_period_end),
    INDEX idx_subscription_external (external_subscription_id),

    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(uid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Subscription Add-ons
-- =====================
CREATE TABLE IF NOT EXISTS subscription_addons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    subscription_id VARCHAR(200) NOT NULL,
    workspace_id VARCHAR(200) NOT NULL,
    addon_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    price DECIMAL(10,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    activated_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    external_item_id VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_addon_uid (uid),
    INDEX idx_addon_subscription (subscription_id),
    INDEX idx_addon_workspace (workspace_id),
    INDEX idx_addon_code (addon_code),
    INDEX idx_addon_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Payment Transactions
-- =====================
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    subscription_id VARCHAR(200) NOT NULL,
    workspace_id VARCHAR(200) NOT NULL,

    -- Provider Info
    payment_provider VARCHAR(30) NOT NULL,
    external_payment_id VARCHAR(255),
    external_invoice_id VARCHAR(255),

    -- Amount
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    tax_amount DECIMAL(10,2) DEFAULT 0,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    net_amount DECIMAL(10,2) NOT NULL DEFAULT 0,

    -- Payment Method
    payment_method_type VARCHAR(30),
    payment_method_last4 VARCHAR(4),
    card_brand VARCHAR(20),

    -- Details
    description VARCHAR(500),
    billing_period_start TIMESTAMP NULL,
    billing_period_end TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,

    -- Failure Info
    failure_reason VARCHAR(500),
    failure_code VARCHAR(100),

    -- Refund Info
    refund_amount DECIMAL(10,2),
    refunded_at TIMESTAMP NULL,
    refund_reason VARCHAR(500),

    -- URLs
    receipt_url VARCHAR(500),
    invoice_pdf_url VARCHAR(500),

    metadata TEXT DEFAULT '{}',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_payment_uid (uid),
    INDEX idx_payment_subscription (subscription_id),
    INDEX idx_payment_workspace (workspace_id),
    INDEX idx_payment_status (status),
    INDEX idx_payment_provider (payment_provider),
    INDEX idx_payment_external (external_payment_id),
    INDEX idx_payment_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Payment Methods
-- =====================
CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(200) NOT NULL,
    payment_provider VARCHAR(30) NOT NULL,
    external_payment_method_id VARCHAR(255) NOT NULL,

    -- Type
    type VARCHAR(30) NOT NULL DEFAULT 'CARD',

    -- Card Info
    last4 VARCHAR(4),
    brand VARCHAR(30),
    exp_month INT,
    exp_year INT,
    cardholder_name VARCHAR(200),

    -- UPI/Banking
    upi_id VARCHAR(100),
    bank_name VARCHAR(100),
    country VARCHAR(2),

    -- Status
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    billing_email VARCHAR(255),
    fingerprint VARCHAR(100),

    metadata TEXT DEFAULT '{}',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_pm_uid (uid),
    INDEX idx_pm_workspace (workspace_id),
    INDEX idx_pm_provider (payment_provider),
    INDEX idx_pm_default (is_default),
    INDEX idx_pm_external (external_payment_method_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Device Registrations
-- =====================
CREATE TABLE IF NOT EXISTS device_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(200) NOT NULL,
    user_id VARCHAR(200) NOT NULL,
    device_id VARCHAR(200) NOT NULL,

    -- Device Info
    device_name VARCHAR(200),
    platform VARCHAR(30) NOT NULL DEFAULT 'ANDROID',
    device_model VARCHAR(100),
    os_version VARCHAR(50),
    app_version VARCHAR(20),

    -- Token
    token_expires_at TIMESTAMP NOT NULL,
    last_sync_at TIMESTAMP NULL,
    last_activity_at TIMESTAMP NULL,

    -- Push Notification
    push_token VARCHAR(500),
    push_token_type VARCHAR(20),

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP NULL,
    deactivation_reason VARCHAR(200),
    last_ip VARCHAR(50),

    metadata TEXT DEFAULT '{}',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_device_uid (uid),
    INDEX idx_device_workspace (workspace_id),
    INDEX idx_device_user (user_id),
    INDEX idx_device_token_expires (token_expires_at),
    INDEX idx_device_active (is_active),
    UNIQUE INDEX uk_device_workspace_device (workspace_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Usage Metrics
-- =====================
CREATE TABLE IF NOT EXISTS usage_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    workspace_id VARCHAR(200) NOT NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,

    -- Resource Counts
    customer_count INT NOT NULL DEFAULT 0,
    product_count INT NOT NULL DEFAULT 0,
    invoice_count INT NOT NULL DEFAULT 0,
    order_count INT NOT NULL DEFAULT 0,
    member_count INT NOT NULL DEFAULT 0,
    device_count INT NOT NULL DEFAULT 0,
    storage_used_bytes BIGINT NOT NULL DEFAULT 0,

    -- API/Notification Usage
    api_calls BIGINT NOT NULL DEFAULT 0,
    sms_count INT NOT NULL DEFAULT 0,
    email_count INT NOT NULL DEFAULT 0,

    -- Limit Flags
    customer_limit_exceeded BOOLEAN NOT NULL DEFAULT FALSE,
    product_limit_exceeded BOOLEAN NOT NULL DEFAULT FALSE,
    invoice_limit_exceeded BOOLEAN NOT NULL DEFAULT FALSE,
    storage_limit_exceeded BOOLEAN NOT NULL DEFAULT FALSE,
    member_limit_exceeded BOOLEAN NOT NULL DEFAULT FALSE,
    device_limit_exceeded BOOLEAN NOT NULL DEFAULT FALSE,

    last_calculated_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_usage_uid (uid),
    INDEX idx_usage_workspace (workspace_id),
    INDEX idx_usage_period (period_year, period_month),
    UNIQUE INDEX uk_usage_workspace_period (workspace_id, period_year, period_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Insert Default Plans
-- =====================
INSERT INTO subscription_plans (
    uid, plan_code, display_name, description,
    monthly_price_inr, monthly_price_usd,
    max_workspaces, max_members_per_workspace, max_storage_gb,
    max_customers, max_products, max_invoices_per_month, max_devices,
    data_retention_years, available_modules,
    api_access_enabled, custom_branding_enabled, sso_enabled, audit_logs_enabled, priority_support,
    trial_days, display_order
) VALUES
-- FREE Plan
('PLAN_FREE_001', 'FREE', 'Free', 'Free tier with basic features',
 0, 0,
 1, 1, 1,
 50, 50, 20, 2,
 1, '["CUSTOMER","PRODUCT","INVOICE"]',
 FALSE, FALSE, FALSE, FALSE, FALSE,
 0, 0),

-- STARTER Plan
('PLAN_STARTER_001', 'STARTER', 'Starter', 'Small business essentials',
 499, 7,
 2, 5, 5,
 500, 500, 200, 5,
 2, '["CUSTOMER","PRODUCT","INVOICE","ORDER"]',
 FALSE, FALSE, FALSE, FALSE, FALSE,
 14, 1),

-- PROFESSIONAL Plan
('PLAN_PRO_001', 'PROFESSIONAL', 'Professional', 'Growing business features',
 1499, 19,
 5, 15, 25,
 -1, -1, -1, 15,
 5, '["CUSTOMER","PRODUCT","INVOICE","ORDER","ANALYTICS","REPORTS"]',
 TRUE, TRUE, FALSE, FALSE, TRUE,
 14, 2),

-- ENTERPRISE Plan
('PLAN_ENT_001', 'ENTERPRISE', 'Enterprise', 'Large organization features',
 4999, 59,
 -1, -1, 100,
 -1, -1, -1, -1,
 -1, '["CUSTOMER","PRODUCT","INVOICE","ORDER","ANALYTICS","REPORTS","INTEGRATIONS","CUSTOM"]',
 TRUE, TRUE, TRUE, TRUE, TRUE,
 30, 3)
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);
