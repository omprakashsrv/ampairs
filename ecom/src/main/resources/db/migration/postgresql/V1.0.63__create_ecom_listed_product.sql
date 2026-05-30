CREATE TABLE ecom_listed_product (
    id                      BIGSERIAL PRIMARY KEY,
    uid                     VARCHAR(200) NOT NULL UNIQUE,
    owner_id                VARCHAR(200),
    ref_id                  VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    storefront_id           VARCHAR(200) NOT NULL,
    management_product_id   VARCHAR(200) NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    image_urls              JSONB        NOT NULL DEFAULT '[]',
    brand                   VARCHAR(255),
    category                VARCHAR(255),
    subcategory             VARCHAR(255),
    price                   NUMERIC(19,4) NOT NULL,
    stock_quantity          INT          NOT NULL DEFAULT 0,
    stock_status            VARCHAR(20)  NOT NULL DEFAULT 'OUT_OF_STOCK',
    is_visible              BOOLEAN      NOT NULL DEFAULT TRUE,
    last_synced_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_ecom_product_storefront UNIQUE (storefront_id, management_product_id)
);

CREATE INDEX idx_ecom_product_storefront      ON ecom_listed_product(storefront_id);
CREATE INDEX idx_ecom_product_management      ON ecom_listed_product(management_product_id);
CREATE INDEX idx_ecom_product_visible         ON ecom_listed_product(storefront_id, is_visible);
CREATE INDEX idx_ecom_product_stock_status    ON ecom_listed_product(storefront_id, stock_status);
