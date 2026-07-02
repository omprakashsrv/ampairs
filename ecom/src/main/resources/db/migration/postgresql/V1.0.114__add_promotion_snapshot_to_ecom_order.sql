-- Ecom Module Extension: Promotion snapshot on orders (PostgreSQL)
-- Description: Captures the offers applied at checkout (015) so the order total is immune to later
--              offer edits. Additive nullable columns — pre-015 orders read null (no promotions).
-- Dependencies: V1.0.109__add_pricing_channel_and_snapshot.sql

ALTER TABLE ecom_order ADD COLUMN promotion_discount_minor BIGINT;
ALTER TABLE ecom_order ADD COLUMN applied_promotions_json TEXT;
