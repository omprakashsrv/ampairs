-- Migration: Create invoice and billing preferences tables for postpaid billing system
-- Version: V1.0.36
-- Description: Adds support for invoice-based postpaid billing with auto-payment and payment links

-- =====================================================
-- Billing Preferences Table
-- =====================================================

CREATE TABLE IF NOT EXISTS billing_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE,
    workspace_id VARCHAR(200) NOT NULL UNIQUE,

    -- Billing Mode
    billing_mode VARCHAR(20) NOT NULL DEFAULT 'PREPAID',
    auto_payment_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    default_payment_method_id BIGINT NULL,

    -- Contact & Currency
    billing_email VARCHAR(255) NOT NULL,
    billing_currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    -- Reminder Settings
    send_payment_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    grace_period_days INT NOT NULL DEFAULT 15,

    -- Tax Information
    tax_identifier VARCHAR(50) NULL,

    -- Billing Address
    billing_address_line1 VARCHAR(255) NULL,
    billing_address_line2 VARCHAR(255) NULL,
    billing_city VARCHAR(100) NULL,
    billing_state VARCHAR(100) NULL,
    billing_postal_code VARCHAR(20) NULL,
    billing_country VARCHAR(2) NULL,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_billing_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Invoices Table
-- =====================================================

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE,

    -- Workspace & Owner
    workspace_id VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,

    -- Invoice Details
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,

    -- Billing Period
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,
    due_date TIMESTAMP NOT NULL,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- Amounts
    subtotal DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    tax_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    discount_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    paid_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    -- Payment Settings
    auto_payment_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    payment_method_id BIGINT NULL,

    -- Payment Provider References
    razorpay_invoice_id VARCHAR(100) NULL,
    stripe_invoice_id VARCHAR(100) NULL,
    payment_link_url VARCHAR(500) NULL,

    -- Timestamps
    generated_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    last_reminder_sent_at TIMESTAMP NULL,
    suspended_at TIMESTAMP NULL,

    -- Reminder Tracking
    reminder_count INT NOT NULL DEFAULT 0,

    -- Notes
    notes VARCHAR(1000) NULL,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_invoice_workspace (workspace_id),
    INDEX idx_invoice_number (invoice_number),
    INDEX idx_invoice_status (status),
    INDEX idx_invoice_due_date (due_date),
    INDEX idx_invoice_subscription (subscription_id),
    INDEX idx_invoice_razorpay (razorpay_invoice_id),
    INDEX idx_invoice_stripe (stripe_invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Invoice Line Items Table
-- =====================================================

CREATE TABLE IF NOT EXISTS invoice_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE,

    -- Invoice Reference
    invoice_id BIGINT NOT NULL,

    -- Line Item Details
    description VARCHAR(500) NOT NULL,
    item_type VARCHAR(50) NULL COMMENT 'SUBSCRIPTION, ADDON, USAGE, DISCOUNT',

    -- Pricing
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,

    -- Tax
    tax_percent DECIMAL(5, 2) NOT NULL DEFAULT 0.00,

    -- References
    usage_record_id BIGINT NULL,
    addon_subscription_id BIGINT NULL,

    -- Period
    period_start TIMESTAMP NULL,
    period_end TIMESTAMP NULL,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_line_item_invoice (invoice_id),
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Update Payment Methods Table
-- =====================================================

-- Add customer ID columns for saving payment methods
ALTER TABLE payment_methods
ADD COLUMN IF NOT EXISTS razorpay_customer_id VARCHAR(100) NULL AFTER external_payment_method_id,
ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(100) NULL AFTER razorpay_customer_id;

-- Add index for customer IDs
CREATE INDEX IF NOT EXISTS idx_pm_razorpay_customer ON payment_methods(razorpay_customer_id);
CREATE INDEX IF NOT EXISTS idx_pm_stripe_customer ON payment_methods(stripe_customer_id);

-- =====================================================
-- Sample Data (Optional - for development)
-- =====================================================

-- Default billing preferences for existing workspaces
-- INSERT INTO billing_preferences (uid, workspace_id, billing_mode, billing_email, billing_currency)
-- SELECT UUID(), workspace_id, 'PREPAID', CONCAT('billing@', workspace_id, '.com'), 'INR'
-- FROM (SELECT DISTINCT workspace_id FROM subscriptions) AS workspaces
-- WHERE NOT EXISTS (SELECT 1 FROM billing_preferences WHERE billing_preferences.workspace_id = workspaces.workspace_id);

-- =====================================================
-- Comments
-- =====================================================

ALTER TABLE invoices COMMENT = 'Stores invoices for postpaid billing system';
ALTER TABLE invoice_line_items COMMENT = 'Stores line items (charges) for each invoice';
ALTER TABLE billing_preferences COMMENT = 'Stores billing preferences and auto-payment settings for workspaces';
