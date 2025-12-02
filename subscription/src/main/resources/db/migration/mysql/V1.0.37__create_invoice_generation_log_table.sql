-- V1.0.37: Create invoice_generation_logs table for tracking invoice generation with idempotency and failure handling
-- This table ensures:
-- 1. Idempotency: One log per workspace per billing period
-- 2. Failure tracking: Tracks errors and retry attempts
-- 3. Reconciliation: Daily job can identify missing/failed invoices
-- 4. Payment tracking: Tracks payment link generation status

CREATE TABLE invoice_generation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes for efficient queries
    INDEX idx_inv_gen_workspace_period (workspace_id, billing_period_year, billing_period_month),
    INDEX idx_inv_gen_status (status),
    INDEX idx_inv_gen_created (created_at),
    INDEX idx_inv_gen_invoice (invoice_id),
    INDEX idx_inv_gen_retry (next_retry_at, should_retry, status),
    INDEX idx_inv_gen_payment_status (payment_status),

    -- Unique constraint for idempotency
    UNIQUE KEY uk_workspace_period (workspace_id, billing_period_year, billing_period_month),

    -- Foreign key to subscription
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE,

    -- Foreign key to invoice (nullable, set when invoice is created)
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comments for documentation
ALTER TABLE invoice_generation_logs COMMENT = 'Tracks invoice generation attempts for idempotency and failure handling';
