-- Add pre-launch discount fields to subscription_plans table

ALTER TABLE subscription_plans
    ADD COLUMN pre_launch_discount_percent INT NOT NULL DEFAULT 0 COMMENT 'Pre-launch discount percentage (0-100)',
    ADD COLUMN pre_launch_discount_end_at TIMESTAMP NULL COMMENT 'Pre-launch discount end date (UTC)',
    ADD COLUMN pre_launch_new_users_only TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Pre-launch discount applies to new users only';

-- Configure 50% pre-launch discount for all paid plans
-- Set end date to your official launch date
-- Example: Launch date is March 1, 2026

UPDATE subscription_plans
SET pre_launch_discount_percent = 50,
    pre_launch_discount_end_at = '2026-03-01 00:00:00',
    pre_launch_new_users_only = 1
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
-- SET pre_launch_new_users_only = 0
-- WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');
