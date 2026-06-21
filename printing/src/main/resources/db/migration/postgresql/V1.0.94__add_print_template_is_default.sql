-- Add is_default flag to print_template (PostgreSQL)
-- Description: marks the chosen template per (document_type, printer_class); synced from the app.

ALTER TABLE print_template ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;
