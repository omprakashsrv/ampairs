-- =====================================================
-- Consolidate the order parties to a single buyer (customer_*). The seller is the implicit
-- current workspace, so the from_customer_* / to_customer_* pair is removed. The buyer's name and
-- GSTIN stay snapshotted on the document. Place of supply (buyer state) is retained.
-- =====================================================

ALTER TABLE customer_order ADD COLUMN customer_gst VARCHAR(30) NOT NULL DEFAULT '';

-- Backfill the single buyer from the legacy to_customer_* party where not already populated.
UPDATE customer_order SET customer_id = to_customer_id
    WHERE (customer_id IS NULL OR customer_id = '') AND to_customer_id <> '';
UPDATE customer_order SET customer_name = to_customer_name
    WHERE (customer_name IS NULL OR customer_name = '') AND to_customer_name <> '';
UPDATE customer_order SET customer_gst = to_customer_gst WHERE to_customer_gst <> '';

ALTER TABLE customer_order
    DROP COLUMN from_customer_id,
    DROP COLUMN from_customer_name,
    DROP COLUMN from_customer_gst,
    DROP COLUMN to_customer_id,
    DROP COLUMN to_customer_name,
    DROP COLUMN to_customer_gst;
