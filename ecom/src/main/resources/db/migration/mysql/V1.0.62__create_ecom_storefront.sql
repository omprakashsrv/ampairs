-- MySQL counterpart of postgresql/V1.0.62 (BIGSERIAL→AUTO_INCREMENT, TIMESTAMPTZ→TIMESTAMP).
CREATE TABLE ecom_storefront (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    uid         VARCHAR(200) NOT NULL,
    owner_id    VARCHAR(200),
    ref_id      VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(50)  NOT NULL,
    description TEXT,
    logo_url    VARCHAR(500),
    banner_url  VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at    TIMESTAMP NULL,
    unpublished_at  TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uq_ecom_storefront_uid (uid),
    UNIQUE INDEX uq_ecom_storefront_slug (slug),
    INDEX idx_ecom_storefront_owner  (owner_id),
    INDEX idx_ecom_storefront_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
