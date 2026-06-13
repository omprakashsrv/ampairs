-- =====================================================
-- Consolidate the invoice parties to a single buyer (customer_*). The seller is the implicit
-- current workspace, so the from_customer_* / to_customer_* pair is removed. The buyer's name and
-- GSTIN stay snapshotted on the tax invoice (frozen at issue time). Place of supply is retained.
-- =====================================================

ALTER TABLE invoice ADD COLUMN customer_id VARCHAR(36);
ALTER TABLE invoice ADD COLUMN customer_name VARCHAR(255);
ALTER TABLE invoice ADD COLUMN customer_phone VARCHAR(20);
ALTER TABLE invoice ADD COLUMN customer_gst VARCHAR(30) NOT NULL DEFAULT '';

-- Backfill the single buyer from the legacy to_customer_* party.
UPDATE invoice SET customer_id = to_customer_id WHERE to_customer_id <> '';
UPDATE invoice SET customer_name = to_customer_name WHERE to_customer_name <> '';
UPDATE invoice SET customer_gst = to_customer_gst WHERE to_customer_gst <> '';

DROP INDEX IF EXISTS idx_invoice_to_customer;
CREATE INDEX idx_invoice_customer ON invoice (customer_id);

ALTER TABLE invoice
    DROP COLUMN from_customer_id,
    DROP COLUMN from_customer_name,
    DROP COLUMN from_customer_gst,
    DROP COLUMN to_customer_id,
    DROP COLUMN to_customer_name,
    DROP COLUMN to_customer_gst;
