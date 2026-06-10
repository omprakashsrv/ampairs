-- MySQL counterpart of postgresql/V1.0.61.
-- PostgreSQL uses a partial unique index (WHERE ecom_order_ref IS NOT NULL); MySQL unique
-- indexes already allow multiple NULLs, so a plain unique index is semantically equivalent.
ALTER TABLE customer_order ADD COLUMN ecom_order_ref VARCHAR(50) NULL;
CREATE UNIQUE INDEX idx_order_ecom_ref ON customer_order(ecom_order_ref);
