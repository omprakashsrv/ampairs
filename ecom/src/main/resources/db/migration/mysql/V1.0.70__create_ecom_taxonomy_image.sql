-- MySQL counterpart of postgresql/V1.0.70.
CREATE TABLE ecom_taxonomy_image (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    uid             VARCHAR(200)  NOT NULL,
    owner_id        VARCHAR(200),
    ref_id          VARCHAR(255),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    storefront_id   VARCHAR(200)  NOT NULL,
    taxonomy_type   VARCHAR(20)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    image_url       VARCHAR(500)  NOT NULL,
    sort_order      INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX uq_ecom_taxonomy_image_uid (uid),
    UNIQUE INDEX uq_ecom_taxonomy (storefront_id, taxonomy_type, name),
    INDEX idx_ecom_taxonomy_storefront (storefront_id, taxonomy_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
