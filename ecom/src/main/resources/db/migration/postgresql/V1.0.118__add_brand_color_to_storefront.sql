-- Optional storefront theme color, stored as a packed ARGB int in a BIGINT (e.g. 0xFF1B6C4A = 4279650378).
ALTER TABLE ecom_storefront
    ADD COLUMN brand_color_argb BIGINT;
