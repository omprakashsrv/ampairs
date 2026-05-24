-- Migrate invoice_date column from TIMESTAMP to TIMESTAMPTZ
ALTER TABLE invoice
    ALTER COLUMN invoice_date TYPE TIMESTAMPTZ USING invoice_date AT TIME ZONE 'UTC';
