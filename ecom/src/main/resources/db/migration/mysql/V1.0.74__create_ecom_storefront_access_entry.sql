-- MySQL counterpart of postgresql/V1.0.74.
CREATE TABLE ecom_storefront_access_entry (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    uid        VARCHAR(200)  NOT NULL,
    owner_id   VARCHAR(200),
    ref_id     VARCHAR(255),
    created_at TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    storefront_id      VARCHAR(200) NOT NULL,
    identifier_type    VARCHAR(20)  NOT NULL,
    identifier_value   VARCHAR(320) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uq_ecom_storefront_access_entry_uid (uid),
    UNIQUE INDEX uq_ecom_storefront_access_entry (storefront_id, identifier_type, identifier_value),
    INDEX idx_ecom_storefront_access_entry_storefront (storefront_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
