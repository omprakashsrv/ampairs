-- Add unit/mrp to listed product for storefront savings display
ALTER TABLE ecom_listed_product
    ADD COLUMN unit  VARCHAR(100),
    ADD COLUMN mrp   NUMERIC(19,4);

-- Add brand/unit/mrp snapshot to cart item for bill breakdown
ALTER TABLE ecom_cart_item
    ADD COLUMN brand       VARCHAR(255),
    ADD COLUMN unit        VARCHAR(100),
    ADD COLUMN mrp_at_add  NUMERIC(19,4);
