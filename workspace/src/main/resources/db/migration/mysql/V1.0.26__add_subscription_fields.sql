-- Workspace Module Database Migration Script
-- Version: 1.0.25
-- Description: Add subscription management fields to workspace
-- Author: Claude Code
-- Date: 2025-11-23
-- Dependencies: V1.0.24__add_workspace_avatar_fields.sql

-- Add subscription-related fields to workspaces table
ALTER TABLE workspaces
    ADD COLUMN subscription_id VARCHAR(200) NULL COMMENT 'Reference to subscription table',
    ADD COLUMN subscription_status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, PAST_DUE, CANCELLED, PAUSED, EXPIRED',
    ADD COLUMN billing_cycle VARCHAR(20) DEFAULT 'MONTHLY' COMMENT 'MONTHLY, QUARTERLY, ANNUAL, BIENNIAL',
    ADD COLUMN payment_provider VARCHAR(30) NULL COMMENT 'GOOGLE_PLAY, APP_STORE, RAZORPAY, STRIPE',
    ADD COLUMN external_customer_id VARCHAR(255) NULL COMMENT 'Customer ID at payment provider',
    ADD COLUMN current_period_start TIMESTAMP NULL COMMENT 'Current billing period start',
    ADD COLUMN current_period_end TIMESTAMP NULL COMMENT 'Current billing period end',
    ADD COLUMN next_billing_amount DECIMAL(10,2) NULL COMMENT 'Amount for next billing',
    ADD COLUMN last_payment_at TIMESTAMP NULL COMMENT 'Last successful payment timestamp',
    ADD COLUMN grace_period_ends_at TIMESTAMP NULL COMMENT 'End of grace period for failed payments';

-- Add indexes for subscription queries
CREATE INDEX idx_workspace_subscription_id ON workspaces(subscription_id);
CREATE INDEX idx_workspace_subscription_status ON workspaces(subscription_status);
CREATE INDEX idx_workspace_period_end ON workspaces(current_period_end);
