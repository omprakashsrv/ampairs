-- =====================================================
-- Store-ops order/invoice (spec 010): invoice line unit of measure + variant,
-- document-level price/discount modes, and client-assigned numbering (series + sequence).
-- =====================================================

-- Invoice line items: unit of measure + base-unit quantity (FR-014) and selected variant
ALTER TABLE invoice_item ADD COLUMN unit_id VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE invoice_item ADD COLUMN base_quantity DOUBLE NOT NULL DEFAULT 0.0;
ALTER TABLE invoice_item ADD COLUMN variant_sku VARCHAR(255) NULL;

-- Invoice document settings (C1: tax basis toggle, C2: overall discount mode)
ALTER TABLE invoice ADD COLUMN price_mode VARCHAR(20) NOT NULL DEFAULT 'TAX_EXCLUSIVE';
ALTER TABLE invoice ADD COLUMN overall_discount_mode VARCHAR(30) NOT NULL DEFAULT 'POST_TAX_REDUCTION';

-- Client-assigned sequential numbering with per-series prefix (C5/FR-B09)
ALTER TABLE invoice ADD COLUMN series VARCHAR(50) NOT NULL DEFAULT 'INV';
ALTER TABLE invoice ADD COLUMN sequence_number BIGINT NOT NULL DEFAULT 0;

-- Backfill existing rows with a unique sequence (id is globally unique) before adding the constraint
UPDATE invoice SET sequence_number = id WHERE sequence_number = 0;

-- (owner_id, series, sequence_number) must be unique per workspace
CREATE UNIQUE INDEX idx_invoice_series_seq ON invoice (owner_id, series, sequence_number);
