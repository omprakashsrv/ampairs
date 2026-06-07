-- =====================================================
-- Store-ops order/invoice (spec 010): line-level unit of measure + selected variant,
-- and document-level price mode + overall discount mode.
-- =====================================================

-- Order line items: unit of measure + base-unit quantity (FR-014) and selected variant
ALTER TABLE order_item ADD COLUMN unit_id VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE order_item ADD COLUMN base_quantity DOUBLE NOT NULL DEFAULT 0.0;
ALTER TABLE order_item ADD COLUMN variant_sku VARCHAR(255) NULL;

-- Order document settings (C1: tax basis toggle, C2: overall discount mode)
ALTER TABLE customer_order ADD COLUMN price_mode VARCHAR(20) NOT NULL DEFAULT 'TAX_EXCLUSIVE';
ALTER TABLE customer_order ADD COLUMN overall_discount_mode VARCHAR(30) NOT NULL DEFAULT 'POST_TAX_REDUCTION';
