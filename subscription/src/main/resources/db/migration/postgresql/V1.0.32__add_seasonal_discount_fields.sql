-- Add seasonal/festival discount fields to subscription_plans table

ALTER TABLE subscription_plans
    ADD COLUMN seasonal_discount_percent INT NOT NULL DEFAULT 0,
    ADD COLUMN seasonal_discount_name VARCHAR(100),
    ADD COLUMN seasonal_discount_start_at TIMESTAMP,
    ADD COLUMN seasonal_discount_end_at TIMESTAMP;

COMMENT ON COLUMN subscription_plans.seasonal_discount_percent IS 'Seasonal discount percentage (0-100)';
COMMENT ON COLUMN subscription_plans.seasonal_discount_name IS 'Discount name (e.g., Diwali Sale, New Year Offer)';
COMMENT ON COLUMN subscription_plans.seasonal_discount_start_at IS 'Discount start date (UTC)';
COMMENT ON COLUMN subscription_plans.seasonal_discount_end_at IS 'Discount end date (UTC)';

-- Example: Configure seasonal discounts for festivals
-- Admins can update these values via API or direct SQL

-- Example Diwali discount (15% off, Oct 10-Nov 5, 2025)
-- UPDATE subscription_plans
-- SET seasonal_discount_percent = 15,
--     seasonal_discount_name = 'Diwali Dhamaka 2025',
--     seasonal_discount_start_at = '2025-10-10 00:00:00',
--     seasonal_discount_end_at = '2025-11-05 23:59:59'
-- WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');

-- Example New Year discount (25% off, Dec 20, 2025 - Jan 10, 2026)
-- UPDATE subscription_plans
-- SET seasonal_discount_percent = 25,
--     seasonal_discount_name = 'New Year Mega Sale',
--     seasonal_discount_start_at = '2025-12-20 00:00:00',
--     seasonal_discount_end_at = '2026-01-10 23:59:59'
-- WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');
