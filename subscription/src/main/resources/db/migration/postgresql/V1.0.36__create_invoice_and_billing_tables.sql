-- Migration: Create invoice and billing preferences tables for postpaid billing system
-- Version: V1.0.36
-- Description: Adds support for invoice-based postpaid billing with auto-payment and payment links
-- Database: PostgreSQL

-- =====================================================
-- Billing Preferences Table
-- =====================================================

CREATE TABLE IF NOT EXISTS billing_preferences (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE,
    ref_id VARCHAR(50) NOT NULL UNIQUE,
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
    grace_period_days INTEGER NOT NULL DEFAULT 15,

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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_billing_workspace ON billing_preferences(workspace_id);

COMMENT ON TABLE billing_preferences IS 'Stores billing preferences and auto-payment settings for workspaces';
COMMENT ON COLUMN billing_preferences.billing_mode IS 'PREPAID or POSTPAID billing mode';
COMMENT ON COLUMN billing_preferences.auto_payment_enabled IS 'Enable automatic payment for invoices';
COMMENT ON COLUMN billing_preferences.grace_period_days IS 'Days before workspace suspension (default 15)';

-- =====================================================
-- Invoices Table
-- =====================================================

CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE,
    ref_id VARCHAR(50) NOT NULL UNIQUE,

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
    subtotal NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    tax_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    discount_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    paid_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
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
    reminder_count INTEGER NOT NULL DEFAULT 0,

    -- Notes
    notes VARCHAR(1000) NULL,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoice_workspace ON invoices(workspace_id);
CREATE INDEX IF NOT EXISTS idx_invoice_number ON invoices(invoice_number);
CREATE INDEX IF NOT EXISTS idx_invoice_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoice_due_date ON invoices(due_date);
CREATE INDEX IF NOT EXISTS idx_invoice_subscription ON invoices(subscription_id);
CREATE INDEX IF NOT EXISTS idx_invoice_razorpay ON invoices(razorpay_invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_stripe ON invoices(stripe_invoice_id);

COMMENT ON TABLE invoices IS 'Stores invoices for postpaid billing system';
COMMENT ON COLUMN invoices.invoice_number IS 'Unique invoice number (e.g., INV-2025-0001)';
COMMENT ON COLUMN invoices.status IS 'DRAFT, PENDING, PAID, PARTIALLY_PAID, OVERDUE, SUSPENDED, FAILED, VOID, REFUNDED';
COMMENT ON COLUMN invoices.auto_payment_enabled IS 'Whether to auto-charge saved payment method';
COMMENT ON COLUMN invoices.payment_link_url IS 'Razorpay/Stripe payment link for manual payment';

-- =====================================================
-- Invoice Line Items Table
-- =====================================================

CREATE TABLE IF NOT EXISTS invoice_line_items (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE,
    ref_id VARCHAR(50) NOT NULL UNIQUE,

    -- Invoice Reference
    invoice_id BIGINT NOT NULL,

    -- Line Item Details
    description VARCHAR(500) NOT NULL,
    item_type VARCHAR(50) NULL,

    -- Pricing
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,

    -- Tax
    tax_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00,

    -- References
    usage_record_id BIGINT NULL,
    addon_subscription_id BIGINT NULL,

    -- Period
    period_start TIMESTAMP NULL,
    period_end TIMESTAMP NULL,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_line_item_invoice ON invoice_line_items(invoice_id);

COMMENT ON TABLE invoice_line_items IS 'Stores line items (charges) for each invoice';
COMMENT ON COLUMN invoice_line_items.item_type IS 'SUBSCRIPTION, ADDON, USAGE, DISCOUNT';

-- =====================================================
-- Update Payment Methods Table
-- =====================================================

-- Add customer ID columns for saving payment methods
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='payment_methods' AND column_name='razorpay_customer_id') THEN
        ALTER TABLE payment_methods ADD COLUMN razorpay_customer_id VARCHAR(100) NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='payment_methods' AND column_name='stripe_customer_id') THEN
        ALTER TABLE payment_methods ADD COLUMN stripe_customer_id VARCHAR(100) NULL;
    END IF;
END $$;

-- Add indexes for customer IDs
CREATE INDEX IF NOT EXISTS idx_pm_razorpay_customer ON payment_methods(razorpay_customer_id);
CREATE INDEX IF NOT EXISTS idx_pm_stripe_customer ON payment_methods(stripe_customer_id);

COMMENT ON COLUMN payment_methods.razorpay_customer_id IS 'Razorpay customer ID for auto-charge';
COMMENT ON COLUMN payment_methods.stripe_customer_id IS 'Stripe customer ID for auto-charge';

-- =====================================================
-- Update Trigger for updated_at (PostgreSQL-specific)
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to billing_preferences
DROP TRIGGER IF EXISTS update_billing_preferences_updated_at ON billing_preferences;
CREATE TRIGGER update_billing_preferences_updated_at
    BEFORE UPDATE ON billing_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to invoices
DROP TRIGGER IF EXISTS update_invoices_updated_at ON invoices;
CREATE TRIGGER update_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to invoice_line_items
DROP TRIGGER IF EXISTS update_invoice_line_items_updated_at ON invoice_line_items;
CREATE TRIGGER update_invoice_line_items_updated_at
    BEFORE UPDATE ON invoice_line_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Sample Data (Optional - for development)
-- =====================================================

-- Default billing preferences for existing workspaces
-- INSERT INTO billing_preferences (uid, workspace_id, billing_mode, billing_email, billing_currency)
-- SELECT gen_random_uuid()::text, workspace_id, 'PREPAID', CONCAT('billing@', workspace_id, '.com'), 'INR'
-- FROM (SELECT DISTINCT workspace_id FROM subscriptions) AS workspaces
-- WHERE NOT EXISTS (SELECT 1 FROM billing_preferences WHERE billing_preferences.workspace_id = workspaces.workspace_id);
