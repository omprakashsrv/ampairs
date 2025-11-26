-- Add pre-launch discount fields to subscription_plans table

ALTER TABLE subscription_plans
    ADD COLUMN pre_launch_discount_percent INT NOT NULL DEFAULT 0,
    ADD COLUMN pre_launch_discount_end_at TIMESTAMP,
    ADD COLUMN pre_launch_new_users_only BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN subscription_plans.pre_launch_discount_percent IS 'Pre-launch discount percentage (0-100)';
COMMENT ON COLUMN subscription_plans.pre_launch_discount_end_at IS 'Pre-launch discount end date (UTC)';
COMMENT ON COLUMN subscription_plans.pre_launch_new_users_only IS 'Pre-launch discount applies to new users only';

-- Configure 50% pre-launch discount for all paid plans
-- Set end date to your official launch date
-- Example: Launch date is March 1, 2026

UPDATE subscription_plans
SET pre_launch_discount_percent = 50,
    pre_launch_discount_end_at = '2026-03-01 00:00:00',
    pre_launch_new_users_only = true
WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');

-- Example usage scenarios:
-- 1. 50% off for all new users joining before launch
-- 2. Early bird special - first 1000 users get lifetime discount
-- 3. Beta tester reward - discount for testing during pre-launch

-- To extend the pre-launch period:
-- UPDATE subscription_plans
-- SET pre_launch_discount_end_at = '2026-06-01 00:00:00'
-- WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');

-- To make it available to all users (not just new):
-- UPDATE subscription_plans
-- SET pre_launch_new_users_only = false
-- WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');
