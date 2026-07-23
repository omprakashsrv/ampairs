-- Order Item soft-delete (PostgreSQL). A removed line rides along on the order /sync push as
-- active = false so the deletion propagates to every device (in-band delete). Existing rows are
-- active by default.
ALTER TABLE order_item ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
