ALTER TABLE customer_order ADD COLUMN ecom_order_ref VARCHAR(50) NULL;
CREATE UNIQUE INDEX idx_order_ecom_ref ON customer_order(ecom_order_ref) WHERE ecom_order_ref IS NOT NULL;
