-- Workspace Module Database Migration Script (PostgreSQL)
-- Version: 1.0.25
-- Description: Add subscription management fields to workspace
-- Author: Claude Code
-- Date: 2025-11-23
-- Dependencies: V1.0.24__add_workspace_avatar_fields.sql

-- Add subscription-related fields to workspaces table
ALTER TABLE workspaces
    ADD COLUMN IF NOT EXISTS subscription_id VARCHAR(200),
    ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(20) DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(20) DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(30),
    ADD COLUMN IF NOT EXISTS external_customer_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS current_period_start TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS current_period_end TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS next_billing_amount DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS last_payment_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS grace_period_ends_at TIMESTAMP WITH TIME ZONE;

-- Add comments
COMMENT ON COLUMN workspaces.subscription_id IS 'Reference to subscription table';
COMMENT ON COLUMN workspaces.subscription_status IS 'ACTIVE, PAST_DUE, CANCELLED, PAUSED, EXPIRED';
COMMENT ON COLUMN workspaces.billing_cycle IS 'MONTHLY, QUARTERLY, ANNUAL, BIENNIAL';
COMMENT ON COLUMN workspaces.payment_provider IS 'GOOGLE_PLAY, APP_STORE, RAZORPAY, STRIPE';
COMMENT ON COLUMN workspaces.external_customer_id IS 'Customer ID at payment provider';
COMMENT ON COLUMN workspaces.current_period_start IS 'Current billing period start';
COMMENT ON COLUMN workspaces.current_period_end IS 'Current billing period end';
COMMENT ON COLUMN workspaces.next_billing_amount IS 'Amount for next billing';
COMMENT ON COLUMN workspaces.last_payment_at IS 'Last successful payment timestamp';
COMMENT ON COLUMN workspaces.grace_period_ends_at IS 'End of grace period for failed payments';

-- Add indexes for subscription queries
CREATE INDEX IF NOT EXISTS idx_workspace_subscription_id ON workspaces(subscription_id);
CREATE INDEX IF NOT EXISTS idx_workspace_subscription_status ON workspaces(subscription_status);
CREATE INDEX IF NOT EXISTS idx_workspace_period_end ON workspaces(current_period_end);
