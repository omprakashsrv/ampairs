-- Tally/offline-imported invoices carry the external number in invoice_number and no app series
-- counter (sequence_number = 0). The full unique index rejected every such invoice after the first
-- (409 on /invoices/sync). MySQL has no partial indexes, so uniqueness is enforced on a functional
-- key that maps 0 to NULL (NULLs never collide in MySQL unique indexes); claimed numbers (> 0)
-- stay unique per (owner, series).
ALTER TABLE invoice DROP INDEX idx_invoice_series_seq;
CREATE UNIQUE INDEX idx_invoice_series_seq ON invoice (owner_id, series, ((NULLIF(sequence_number, 0))));
