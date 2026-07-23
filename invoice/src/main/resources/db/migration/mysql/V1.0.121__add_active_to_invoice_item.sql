-- Invoice Item soft-delete (MySQL). A removed line rides along on the invoice /sync push as
-- active = false so the deletion propagates to every device (in-band delete). Existing rows are
-- active by default.
ALTER TABLE invoice_item ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
