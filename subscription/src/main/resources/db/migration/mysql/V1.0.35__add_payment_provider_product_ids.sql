-- Migration: V1.0.35 - Add payment provider product IDs
-- Purpose: Configure product/plan/price IDs for all payment providers
-- Date: 2025-01-27

-- =====================================================
-- Add Product ID Columns
-- =====================================================

-- Google Play product IDs
ALTER TABLE subscription_plans
ADD COLUMN IF NOT EXISTS google_play_product_id_monthly VARCHAR(255) COMMENT 'Google Play product ID for monthly billing',
ADD COLUMN IF NOT EXISTS google_play_product_id_annual VARCHAR(255) COMMENT 'Google Play product ID for annual billing';

-- Apple App Store product IDs
ALTER TABLE subscription_plans
ADD COLUMN IF NOT EXISTS app_store_product_id_monthly VARCHAR(255) COMMENT 'App Store product ID for monthly billing',
ADD COLUMN IF NOT EXISTS app_store_product_id_annual VARCHAR(255) COMMENT 'App Store product ID for annual billing';

-- Razorpay plan IDs
ALTER TABLE subscription_plans
ADD COLUMN IF NOT EXISTS razorpay_plan_id_monthly VARCHAR(255) COMMENT 'Razorpay plan ID for monthly billing',
ADD COLUMN IF NOT EXISTS razorpay_plan_id_annual VARCHAR(255) COMMENT 'Razorpay plan ID for annual billing';

-- Stripe price IDs
ALTER TABLE subscription_plans
ADD COLUMN IF NOT EXISTS stripe_price_id_monthly VARCHAR(255) COMMENT 'Stripe price ID for monthly billing',
ADD COLUMN IF NOT EXISTS stripe_price_id_annual VARCHAR(255) COMMENT 'Stripe price ID for annual billing';

-- =====================================================
-- Update FREE Plan (No Payment Provider IDs)
-- =====================================================
UPDATE subscription_plans
SET
  google_play_product_id_monthly = NULL,
  google_play_product_id_annual = NULL,
  app_store_product_id_monthly = NULL,
  app_store_product_id_annual = NULL,
  razorpay_plan_id_monthly = NULL,
  razorpay_plan_id_annual = NULL,
  stripe_price_id_monthly = NULL,
  stripe_price_id_annual = NULL
WHERE plan_code = 'FREE';

-- =====================================================
-- Update PROFESSIONAL Plan
-- =====================================================
-- NOTE: Replace PLACEHOLDER values with actual IDs from payment provider dashboards
-- For initial deployment, these placeholders will cause runtime errors until replaced

UPDATE subscription_plans
SET
  -- Google Play (format: packagename_plan_cycle)
  google_play_product_id_monthly = 'ampairs_professional_monthly',
  google_play_product_id_annual = 'ampairs_professional_annual',

  -- Apple App Store (format: com.companyname.productname.cycle)
  app_store_product_id_monthly = 'com.ampairs.professional.monthly',
  app_store_product_id_annual = 'com.ampairs.professional.annual',

  -- Razorpay (format: plan_XXXXXXXXXXXX from Razorpay Dashboard)
  razorpay_plan_id_monthly = 'RAZORPAY_PLAN_ID_MONTHLY_PLACEHOLDER',  -- TODO: Replace with actual Razorpay plan ID
  razorpay_plan_id_annual = 'RAZORPAY_PLAN_ID_ANNUAL_PLACEHOLDER',    -- TODO: Replace with actual Razorpay plan ID

  -- Stripe (format: price_XXXXXXXXXXXX from Stripe Dashboard)
  stripe_price_id_monthly = 'STRIPE_PRICE_ID_MONTHLY_PLACEHOLDER',    -- TODO: Replace with actual Stripe price ID
  stripe_price_id_annual = 'STRIPE_PRICE_ID_ANNUAL_PLACEHOLDER'        -- TODO: Replace with actual Stripe price ID

WHERE plan_code = 'PROFESSIONAL';

-- =====================================================
-- Update BUSINESS Plan (if exists)
-- =====================================================
UPDATE subscription_plans
SET
  google_play_product_id_monthly = 'ampairs_business_monthly',
  google_play_product_id_annual = 'ampairs_business_annual',
  app_store_product_id_monthly = 'com.ampairs.business.monthly',
  app_store_product_id_annual = 'com.ampairs.business.annual',
  razorpay_plan_id_monthly = 'RAZORPAY_PLAN_ID_MONTHLY_PLACEHOLDER',
  razorpay_plan_id_annual = 'RAZORPAY_PLAN_ID_ANNUAL_PLACEHOLDER',
  stripe_price_id_monthly = 'STRIPE_PRICE_ID_MONTHLY_PLACEHOLDER',
  stripe_price_id_annual = 'STRIPE_PRICE_ID_ANNUAL_PLACEHOLDER'
WHERE plan_code = 'BUSINESS';

-- =====================================================
-- Update ENTERPRISE Plan (if exists)
-- =====================================================
UPDATE subscription_plans
SET
  google_play_product_id_monthly = 'ampairs_enterprise_monthly',
  google_play_product_id_annual = 'ampairs_enterprise_annual',
  app_store_product_id_monthly = 'com.ampairs.enterprise.monthly',
  app_store_product_id_annual = 'com.ampairs.enterprise.annual',
  razorpay_plan_id_monthly = 'RAZORPAY_PLAN_ID_MONTHLY_PLACEHOLDER',
  razorpay_plan_id_annual = 'RAZORPAY_PLAN_ID_ANNUAL_PLACEHOLDER',
  stripe_price_id_monthly = 'STRIPE_PRICE_ID_MONTHLY_PLACEHOLDER',
  stripe_price_id_annual = 'STRIPE_PRICE_ID_ANNUAL_PLACEHOLDER'
WHERE plan_code = 'ENTERPRISE';

-- =====================================================
-- Indexes for Performance
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_subscription_plans_google_play_monthly
ON subscription_plans(google_play_product_id_monthly);

CREATE INDEX IF NOT EXISTS idx_subscription_plans_google_play_annual
ON subscription_plans(google_play_product_id_annual);

CREATE INDEX IF NOT EXISTS idx_subscription_plans_app_store_monthly
ON subscription_plans(app_store_product_id_monthly);

CREATE INDEX IF NOT EXISTS idx_subscription_plans_app_store_annual
ON subscription_plans(app_store_product_id_annual);

-- =====================================================
-- IMPORTANT INSTRUCTIONS FOR DEPLOYMENT
-- =====================================================
--
-- Before deploying to production, you MUST:
--
-- 1. Google Play Console (play.google.com/console):
--    - Create subscription products
--    - Use IDs already set above (ampairs_professional_monthly, etc.)
--    - Set pricing for each market
--    - Configure billing period
--
-- 2. App Store Connect (appstoreconnect.apple.com):
--    - Create in-app purchase subscriptions
--    - Use IDs already set above (com.ampairs.professional.monthly, etc.)
--    - Set pricing for each territory
--    - Submit for review
--
-- 3. Razorpay Dashboard (dashboard.razorpay.com):
--    - Go to Subscriptions > Plans
--    - Create new subscription plan
--    - Copy the plan_id (e.g., plan_Ja4unjXZUeCT3g)
--    - Run UPDATE query to replace PLACEHOLDER:
--      UPDATE subscription_plans
--      SET razorpay_plan_id_monthly = 'plan_YOUR_ACTUAL_ID'
--      WHERE plan_code = 'PROFESSIONAL';
--
-- 4. Stripe Dashboard (dashboard.stripe.com):
--    - Go to Products > Add product
--    - Create subscription product
--    - Add monthly and annual prices
--    - Copy the price_id (e.g., price_1JqXXXXXXXXXXXXX)
--    - Run UPDATE query to replace PLACEHOLDER:
--      UPDATE subscription_plans
--      SET stripe_price_id_monthly = 'price_YOUR_ACTUAL_ID'
--      WHERE plan_code = 'PROFESSIONAL';
--
-- 5. Verify Configuration:
--    SELECT plan_code,
--           google_play_product_id_monthly,
--           app_store_product_id_monthly,
--           razorpay_plan_id_monthly,
--           stripe_price_id_monthly
--    FROM subscription_plans
--    WHERE plan_code != 'FREE';
--
-- WARNING: Using PLACEHOLDER values will cause payment flows to fail!
-- =====================================================
