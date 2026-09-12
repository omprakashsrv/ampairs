-- invoice_number has been globally unique since V1.0.11, but numbering is client-assigned per
-- workspace (series/sequence_number, spec 010 C5/FR-B09) — two independent workspaces both syncing
-- their first "INV"-series invoice both compute invoice_number = 'INV-1' and collide on this
-- constraint (409 on /invoices/sync). Real numbering uniqueness is already enforced per workspace
-- by idx_invoice_series_seq (V1.0.119); invoice_number itself doesn't need a DB-level constraint.
ALTER TABLE invoice DROP CONSTRAINT invoice_invoice_number_key;
