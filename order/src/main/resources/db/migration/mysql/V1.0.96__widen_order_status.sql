-- Widen status to fit longer ecom lifecycle values. The original VARCHAR(20) overflows on
-- 'PENDING_MERCHANT_REVIEW' (23 chars), used when a storefront order is ingested into the
-- order module.
ALTER TABLE customer_order MODIFY COLUMN status VARCHAR(40) NOT NULL DEFAULT 'DRAFT';
