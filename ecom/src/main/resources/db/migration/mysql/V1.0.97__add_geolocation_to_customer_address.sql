-- Optional drop-point geolocation captured when a buyer picks the address on the map.
ALTER TABLE ecom_customer_address ADD COLUMN latitude DOUBLE NULL;
ALTER TABLE ecom_customer_address ADD COLUMN longitude DOUBLE NULL;
