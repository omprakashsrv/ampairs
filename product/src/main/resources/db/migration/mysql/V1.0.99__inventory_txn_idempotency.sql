-- Product/Inventory Module Migration (MySQL counterpart of postgresql/V1.0.99)
-- Spec 014 (Inventory Revamp) — order/invoice -> stock idempotency.
-- Adds source_type/source_id/source_line_uid to inventory_transaction.
--
-- NOTE: MySQL has NO partial/filtered unique index, so the
-- "(source_type, source_id, source_line_uid, owner_id) WHERE source_line_uid IS NOT NULL"
-- guarantee from the PostgreSQL migration cannot be expressed here. The runtime/dev DB is
-- PostgreSQL (where the partial index is the backstop); on MySQL the application-level
-- skip-if-exists guard in InventoryStockServiceImpl is the authoritative idempotency mechanism.
-- A plain UNIQUE here would wrongly constrain the many rows with NULL source_line_uid, so we add
-- only a non-unique lookup index.

ALTER TABLE inventory_transaction ADD COLUMN source_type     VARCHAR(50)  NULL;
ALTER TABLE inventory_transaction ADD COLUMN source_id       VARCHAR(200) NULL;
ALTER TABLE inventory_transaction ADD COLUMN source_line_uid VARCHAR(200) NULL;

CREATE INDEX idx_inv_txn_source        ON inventory_transaction (source_type, source_id);
CREATE INDEX idx_inv_txn_idem_lookup   ON inventory_transaction (source_type, source_id, source_line_uid);
