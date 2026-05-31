CREATE TABLE ecom_cart (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(200) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    storefront_id   VARCHAR(200) NOT NULL,
    customer_id     VARCHAR(200),
    session_token   VARCHAR(200) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_ecom_cart_session    ON ecom_cart(session_token);
CREATE INDEX idx_ecom_cart_customer   ON ecom_cart(customer_id);
CREATE INDEX idx_ecom_cart_storefront ON ecom_cart(storefront_id);
CREATE INDEX idx_ecom_cart_expires    ON ecom_cart(expires_at);

CREATE TABLE ecom_cart_item (
    id                      BIGSERIAL PRIMARY KEY,
    uid                     VARCHAR(200) NOT NULL UNIQUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cart_id                 VARCHAR(200) NOT NULL,
    listed_product_id       VARCHAR(200) NOT NULL,
    management_product_id   VARCHAR(200) NOT NULL,
    product_name            VARCHAR(255) NOT NULL,
    unit_price              NUMERIC(19,4) NOT NULL,
    quantity                INT          NOT NULL,
    primary_image_url       VARCHAR(500)
);

CREATE INDEX idx_ecom_cart_item_cart ON ecom_cart_item(cart_id);
