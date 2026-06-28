-- Ecom Pricing Integration (MySQL)
-- Description: Storefront sales channel + price-resolution snapshot columns on cart/order lines (009).

ALTER TABLE ecom_storefront ADD COLUMN default_channel VARCHAR(30) NOT NULL DEFAULT 'RETAIL';

ALTER TABLE ecom_cart_item ADD COLUMN resolved_unit_price_minor BIGINT NULL;
ALTER TABLE ecom_cart_item ADD COLUMN currency VARCHAR(3) NULL;
ALTER TABLE ecom_cart_item ADD COLUMN price_source VARCHAR(30) NULL;
ALTER TABLE ecom_cart_item ADD COLUMN matched_price_list_uid VARCHAR(200) NULL;

ALTER TABLE ecom_order_line_item ADD COLUMN resolved_unit_price_minor BIGINT NULL;
ALTER TABLE ecom_order_line_item ADD COLUMN currency VARCHAR(3) NULL;
ALTER TABLE ecom_order_line_item ADD COLUMN price_source VARCHAR(30) NULL;
ALTER TABLE ecom_order_line_item ADD COLUMN matched_price_list_uid VARCHAR(200) NULL;
ALTER TABLE ecom_order_line_item ADD COLUMN applied_tier_min_qty DECIMAL(19,4) NULL;
ALTER TABLE ecom_order_line_item ADD COLUMN below_moq BOOLEAN NULL;
