-- Links an order back to the cart it was created from so checkout is idempotent:
-- a retried checkout (double-tap / lost success response) returns the existing order
-- instead of creating a duplicate.
ALTER TABLE ecom_order ADD COLUMN source_cart_token VARCHAR(200) NULL;
CREATE INDEX idx_ecom_order_source_cart_token ON ecom_order (source_cart_token);
