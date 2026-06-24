-- Invoice Item pricing snapshot (MySQL) — client-resolved price persisted verbatim (009).
ALTER TABLE invoice_item ADD COLUMN resolved_unit_price_minor BIGINT NULL;
ALTER TABLE invoice_item ADD COLUMN currency VARCHAR(3) NULL;
ALTER TABLE invoice_item ADD COLUMN price_source VARCHAR(30) NULL;
ALTER TABLE invoice_item ADD COLUMN matched_price_list_uid VARCHAR(200) NULL;
ALTER TABLE invoice_item ADD COLUMN applied_tier_min_qty DOUBLE NULL;
ALTER TABLE invoice_item ADD COLUMN below_moq BOOLEAN NULL;
