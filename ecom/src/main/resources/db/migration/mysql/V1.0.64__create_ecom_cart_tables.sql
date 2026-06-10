-- MySQL counterpart of postgresql/V1.0.64.
CREATE TABLE ecom_cart (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    uid             VARCHAR(200) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    storefront_id   VARCHAR(200) NOT NULL,
    customer_id     VARCHAR(200),
    session_token   VARCHAR(200) NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE INDEX uq_ecom_cart_uid (uid),
    UNIQUE INDEX uq_ecom_cart_session (session_token),
    INDEX idx_ecom_cart_customer   (customer_id),
    INDEX idx_ecom_cart_storefront (storefront_id),
    INDEX idx_ecom_cart_expires    (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ecom_cart_item (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    uid                     VARCHAR(200) NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    cart_id                 VARCHAR(200) NOT NULL,
    listed_product_id       VARCHAR(200) NOT NULL,
    management_product_id   VARCHAR(200) NOT NULL,
    product_name            VARCHAR(255) NOT NULL,
    unit_price              DECIMAL(19,4) NOT NULL,
    quantity                INT          NOT NULL,
    primary_image_url       VARCHAR(500),
    PRIMARY KEY (id),
    UNIQUE INDEX uq_ecom_cart_item_uid (uid),
    INDEX idx_ecom_cart_item_cart (cart_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
