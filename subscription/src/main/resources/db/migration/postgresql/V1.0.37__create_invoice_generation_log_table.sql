-- V1.0.37: Create invoice_generation_logs table for tracking invoice generation with idempotency and failure handling (PostgreSQL)
-- This table ensures:
-- 1. Idempotency: One log per workspace per billing period
-- 2. Failure tracking: Tracks errors and retry attempts
-- 3. Reconciliation: Daily job can identify missing/failed invoices
-- 4. Payment tracking: Tracks payment link generation status

CREATE TABLE invoice_generation_logs (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    ref_id VARCHAR(200) NOT NULL UNIQUE,

    -- Workspace and subscription tracking
    workspace_id VARCHAR(200) NOT NULL,
    subscription_id BIGINT NOT NULL,

    -- Billing period identification (for idempotency)
    billing_period_year INT NOT NULL,
    billing_period_month INT NOT NULL,
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,

    -- Generation status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING, IN_PROGRESS, SUCCESS, FAILED, SKIPPED

    -- Result tracking
    invoice_id BIGINT NULL,
    invoice_number VARCHAR(50) NULL,

    -- Retry mechanism
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP NULL,
    succeeded_at TIMESTAMP NULL,

    -- Error tracking
    error_message TEXT NULL,
    error_stack_trace TEXT NULL,

    -- Payment processing tracking
    payment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    -- NOT_STARTED, AUTO_CHARGING, AUTO_CHARGE_SUCCESS, AUTO_CHARGE_FAILED,
    -- LINK_GENERATING, LINK_SENT, LINK_FAILED
    payment_link_sent_at TIMESTAMP NULL,
    payment_link_error TEXT NULL,

    -- Retry scheduling (exponential backoff)
    next_retry_at TIMESTAMP NULL,
    should_retry BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint for idempotency
    CONSTRAINT uk_workspace_period UNIQUE (workspace_id, billing_period_year, billing_period_month),

    -- Foreign key to subscription
    CONSTRAINT fk_inv_gen_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscriptions(id) ON DELETE CASCADE,

    -- Foreign key to invoice (nullable, set when invoice is created)
    CONSTRAINT fk_inv_gen_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE SET NULL
);

-- Indexes for efficient queries
CREATE INDEX idx_inv_gen_workspace_period ON invoice_generation_logs (workspace_id, billing_period_year, billing_period_month);
CREATE INDEX idx_inv_gen_status ON invoice_generation_logs (status);
CREATE INDEX idx_inv_gen_created ON invoice_generation_logs (created_at);
CREATE INDEX idx_inv_gen_invoice ON invoice_generation_logs (invoice_id);
CREATE INDEX idx_inv_gen_retry ON invoice_generation_logs (next_retry_at, should_retry, status);
CREATE INDEX idx_inv_gen_payment_status ON invoice_generation_logs (payment_status);

-- Trigger for automatic updated_at timestamp
CREATE OR REPLACE FUNCTION update_invoice_generation_logs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_invoice_generation_logs_updated_at
    BEFORE UPDATE ON invoice_generation_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_invoice_generation_logs_updated_at();

-- Add table comment for documentation
COMMENT ON TABLE invoice_generation_logs IS 'Tracks invoice generation attempts for idempotency and failure handling';
COMMENT ON COLUMN invoice_generation_logs.status IS 'Generation status: PENDING, IN_PROGRESS, SUCCESS, FAILED, SKIPPED';
COMMENT ON COLUMN invoice_generation_logs.payment_status IS 'Payment processing status: NOT_STARTED, AUTO_CHARGING, AUTO_CHARGE_SUCCESS, AUTO_CHARGE_FAILED, LINK_GENERATING, LINK_SENT, LINK_FAILED';
COMMENT ON COLUMN invoice_generation_logs.attempt_count IS 'Number of generation attempts (max 5 before manual intervention required)';
COMMENT ON COLUMN invoice_generation_logs.next_retry_at IS 'Scheduled retry timestamp with exponential backoff';
