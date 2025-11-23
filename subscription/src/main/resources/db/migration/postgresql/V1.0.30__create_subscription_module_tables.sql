-- Subscription Module Database Migration Script (PostgreSQL)
-- Version: 1.0.30
-- Description: Create subscription module tables
-- Author: Claude Code
-- Date: 2025-11-23
-- Dependencies: V1.0.5__create_workspace_module_tables.sql

-- =====================
-- Subscription Plan Definition (Master Data)
-- =====================
CREATE TABLE IF NOT EXISTS subscription_plans (
    id BIGSERIAL PRIMARY KEY,
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

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sub_plan_uid ON subscription_plans(uid);
CREATE INDEX IF NOT EXISTS idx_sub_plan_code ON subscription_plans(plan_code);
CREATE INDEX IF NOT EXISTS idx_sub_plan_active ON subscription_plans(active);

-- =====================
-- Subscriptions
-- =====================
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
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
    current_period_start TIMESTAMP WITH TIME ZONE,
    current_period_end TIMESTAMP WITH TIME ZONE,
    trial_ends_at TIMESTAMP WITH TIME ZONE,

    -- Cancellation
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(500),

    -- Pause
    paused_at TIMESTAMP WITH TIME ZONE,
    resume_at TIMESTAMP WITH TIME ZONE,

    -- Payment Status
    next_billing_amount DECIMAL(10,2),
    last_payment_status VARCHAR(20),
    last_payment_at TIMESTAMP WITH TIME ZONE,
    failed_payment_count INT NOT NULL DEFAULT 0,
    grace_period_ends_at TIMESTAMP WITH TIME ZONE,

    -- Flags
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    metadata TEXT DEFAULT '{}',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(uid) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_subscription_uid ON subscriptions(uid);
CREATE INDEX IF NOT EXISTS idx_subscription_workspace ON subscriptions(workspace_id);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_subscription_provider ON subscriptions(payment_provider);
CREATE INDEX IF NOT EXISTS idx_subscription_period_end ON subscriptions(current_period_end);
CREATE INDEX IF NOT EXISTS idx_subscription_external ON subscriptions(external_subscription_id);

-- =====================
-- Subscription Add-ons
-- =====================
CREATE TABLE IF NOT EXISTS subscription_addons (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    subscription_id VARCHAR(200) NOT NULL,
    workspace_id VARCHAR(200) NOT NULL,
    addon_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    price DECIMAL(10,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    activated_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    external_item_id VARCHAR(255),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_addon_uid ON subscription_addons(uid);
CREATE INDEX IF NOT EXISTS idx_addon_subscription ON subscription_addons(subscription_id);
CREATE INDEX IF NOT EXISTS idx_addon_workspace ON subscription_addons(workspace_id);
CREATE INDEX IF NOT EXISTS idx_addon_code ON subscription_addons(addon_code);
CREATE INDEX IF NOT EXISTS idx_addon_status ON subscription_addons(status);

-- =====================
-- Payment Transactions
-- =====================
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGSERIAL PRIMARY KEY,
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
    billing_period_start TIMESTAMP WITH TIME ZONE,
    billing_period_end TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,

    -- Failure Info
    failure_reason VARCHAR(500),
    failure_code VARCHAR(100),

    -- Refund Info
    refund_amount DECIMAL(10,2),
    refunded_at TIMESTAMP WITH TIME ZONE,
    refund_reason VARCHAR(500),

    -- URLs
    receipt_url VARCHAR(500),
    invoice_pdf_url VARCHAR(500),

    metadata TEXT DEFAULT '{}',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_uid ON payment_transactions(uid);
CREATE INDEX IF NOT EXISTS idx_payment_subscription ON payment_transactions(subscription_id);
CREATE INDEX IF NOT EXISTS idx_payment_workspace ON payment_transactions(workspace_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_payment_provider ON payment_transactions(payment_provider);
CREATE INDEX IF NOT EXISTS idx_payment_external ON payment_transactions(external_payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_created ON payment_transactions(created_at);

-- =====================
-- Payment Methods
-- =====================
CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGSERIAL PRIMARY KEY,
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

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pm_uid ON payment_methods(uid);
CREATE INDEX IF NOT EXISTS idx_pm_workspace ON payment_methods(workspace_id);
CREATE INDEX IF NOT EXISTS idx_pm_provider ON payment_methods(payment_provider);
CREATE INDEX IF NOT EXISTS idx_pm_default ON payment_methods(is_default);
CREATE INDEX IF NOT EXISTS idx_pm_external ON payment_methods(external_payment_method_id);

-- =====================
-- Device Registrations
-- =====================
CREATE TABLE IF NOT EXISTS device_registrations (
    id BIGSERIAL PRIMARY KEY,
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
    token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    last_activity_at TIMESTAMP WITH TIME ZONE,

    -- Push Notification
    push_token VARCHAR(500),
    push_token_type VARCHAR(20),

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    deactivation_reason VARCHAR(200),
    last_ip VARCHAR(50),

    metadata TEXT DEFAULT '{}',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_device_workspace_device UNIQUE (workspace_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_device_uid ON device_registrations(uid);
CREATE INDEX IF NOT EXISTS idx_device_workspace ON device_registrations(workspace_id);
CREATE INDEX IF NOT EXISTS idx_device_user ON device_registrations(user_id);
CREATE INDEX IF NOT EXISTS idx_device_token_expires ON device_registrations(token_expires_at);
CREATE INDEX IF NOT EXISTS idx_device_active ON device_registrations(is_active);

-- =====================
-- Usage Metrics
-- =====================
CREATE TABLE IF NOT EXISTS usage_metrics (
    id BIGSERIAL PRIMARY KEY,
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

    last_calculated_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_usage_workspace_period UNIQUE (workspace_id, period_year, period_month)
);

CREATE INDEX IF NOT EXISTS idx_usage_uid ON usage_metrics(uid);
CREATE INDEX IF NOT EXISTS idx_usage_workspace ON usage_metrics(workspace_id);
CREATE INDEX IF NOT EXISTS idx_usage_period ON usage_metrics(period_year, period_month);

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
ON CONFLICT (plan_code) DO UPDATE SET display_name = EXCLUDED.display_name;

-- =====================
-- Create updated_at trigger function
-- =====================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to all tables
DROP TRIGGER IF EXISTS update_subscription_plans_updated_at ON subscription_plans;
CREATE TRIGGER update_subscription_plans_updated_at
    BEFORE UPDATE ON subscription_plans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_subscriptions_updated_at ON subscriptions;
CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_subscription_addons_updated_at ON subscription_addons;
CREATE TRIGGER update_subscription_addons_updated_at
    BEFORE UPDATE ON subscription_addons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_payment_transactions_updated_at ON payment_transactions;
CREATE TRIGGER update_payment_transactions_updated_at
    BEFORE UPDATE ON payment_transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_payment_methods_updated_at ON payment_methods;
CREATE TRIGGER update_payment_methods_updated_at
    BEFORE UPDATE ON payment_methods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_device_registrations_updated_at ON device_registrations;
CREATE TRIGGER update_device_registrations_updated_at
    BEFORE UPDATE ON device_registrations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_usage_metrics_updated_at ON usage_metrics;
CREATE TRIGGER update_usage_metrics_updated_at
    BEFORE UPDATE ON usage_metrics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
