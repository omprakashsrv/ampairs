-- V1.0.123 added order_number as nullable. Rows created before that migration (or before checkout
-- started assigning it) still have NULL, and the entity/response types are non-null Strings, which
-- crashes with an NPE when Hibernate loads such a row. Backfill and enforce NOT NULL to close the gap.
UPDATE ecom_order SET order_number = '' WHERE order_number IS NULL;
ALTER TABLE ecom_order ALTER COLUMN order_number SET DEFAULT '';
ALTER TABLE ecom_order ALTER COLUMN order_number SET NOT NULL;
