-- Product/Inventory Module Migration (PostgreSQL)
-- Version: 1.0.99
-- Spec 014 (Inventory Revamp) — order/invoice -> stock idempotency.
-- Adds source_type/source_id/source_line_uid to inventory_transaction and a PARTIAL unique index
-- guaranteeing a retried/duplicated sale event never double-counts (R2 / SC-002).

ALTER TABLE inventory_transaction ADD COLUMN IF NOT EXISTS source_type     VARCHAR(50);
ALTER TABLE inventory_transaction ADD COLUMN IF NOT EXISTS source_id       VARCHAR(200);
ALTER TABLE inventory_transaction ADD COLUMN IF NOT EXISTS source_line_uid VARCHAR(200);

-- Partial unique index: one movement per (source_type, source_id, source_line_uid) within a tenant.
-- Manual/count movements leave source_line_uid NULL and are exempt.
CREATE UNIQUE INDEX IF NOT EXISTS ux_inv_txn_idem
    ON inventory_transaction (source_type, source_id, source_line_uid, owner_id)
    WHERE source_line_uid IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inv_txn_source
    ON inventory_transaction (source_type, source_id);
