-- Seller (issuing business) identity snapshotted on the invoice at issue time, so the tax invoice
-- is self-contained and frozen: name/address from the business profile, GSTIN from the tax registration.
ALTER TABLE invoice ADD COLUMN seller_name VARCHAR(255);
ALTER TABLE invoice ADD COLUMN seller_address VARCHAR(1000);
ALTER TABLE invoice ADD COLUMN seller_gst VARCHAR(30);
