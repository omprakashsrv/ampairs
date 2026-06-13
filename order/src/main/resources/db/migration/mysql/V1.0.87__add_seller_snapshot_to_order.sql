-- Seller (issuing business) identity snapshotted on the order at issue time, so the document is
-- self-contained: name/address from the business profile, GSTIN from the workspace tax registration.
ALTER TABLE customer_order ADD COLUMN seller_name VARCHAR(255);
ALTER TABLE customer_order ADD COLUMN seller_address VARCHAR(1000);
ALTER TABLE customer_order ADD COLUMN seller_gst VARCHAR(30);
