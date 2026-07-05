-- Tally/offline-imported invoices carry the external number in invoice_number and no app series
-- counter (sequence_number = 0). The full unique index rejected every such invoice after the first
-- (409 on /invoices/sync). Only claimed sequence numbers (> 0) must be unique per (owner, series);
-- the service-level collision check in InvoiceService.bulkUpsertInvoices already skips seq 0.
DROP INDEX IF EXISTS idx_invoice_series_seq;
CREATE UNIQUE INDEX idx_invoice_series_seq ON invoice (owner_id, series, sequence_number)
    WHERE sequence_number > 0;
