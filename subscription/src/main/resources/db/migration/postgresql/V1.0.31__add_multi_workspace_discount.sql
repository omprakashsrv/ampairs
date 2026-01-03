-- Add multi-workspace discount fields to subscription_plans table

ALTER TABLE subscription_plans
    ADD COLUMN multi_workspace_min_count INT NOT NULL DEFAULT 0,
    ADD COLUMN multi_workspace_discount_percent INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN subscription_plans.multi_workspace_min_count IS 'Minimum workspaces required for discount';
COMMENT ON COLUMN subscription_plans.multi_workspace_discount_percent IS 'Discount percentage (0-100) for multiple workspaces';

-- Update existing plans with default discount policy (example: 20% off for 3+ workspaces)
-- Admins can customize these values per plan later

UPDATE subscription_plans
SET multi_workspace_min_count = 3,
    multi_workspace_discount_percent = 20
WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');

-- FREE plan doesn't need discount (already free)
UPDATE subscription_plans
SET multi_workspace_min_count = 0,
    multi_workspace_discount_percent = 0
WHERE plan_code = 'FREE';
