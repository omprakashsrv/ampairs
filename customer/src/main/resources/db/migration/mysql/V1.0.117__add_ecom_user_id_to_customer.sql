-- Links a CRM customer to the ecom storefront buyer (auth user) it was created from, so repeat
-- storefront orders by the same shopper resolve to one customer.
ALTER TABLE customer ADD COLUMN ecom_user_id VARCHAR(200) NULL;
CREATE INDEX idx_customer_ecom_user_id ON customer (owner_id, ecom_user_id);
