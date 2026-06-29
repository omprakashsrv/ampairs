-- Trade Module Migration (MySQL)
-- Version: 1.0.118
-- Description: Cross-tenant network & consent edge — networks, links (+ consent scope), retailers,
--              Hop-A/Hop-B product attribution, scheme publications, primary-order handshake.

CREATE TABLE trade_networks
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    uid                VARCHAR(40)  NOT NULL,
    brand_workspace_id VARCHAR(40)  NOT NULL,
    name               VARCHAR(150),
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_trade_network_uid (uid),
    INDEX idx_trade_network_brand (brand_workspace_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE trade_links
(
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    uid                      VARCHAR(40) NOT NULL,
    brand_workspace_id       VARCHAR(40) NOT NULL,
    distributor_workspace_id VARCHAR(40) NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'INVITED',
    retailer_visibility      VARCHAR(20) NOT NULL DEFAULT 'CODED',
    share_secondary_sales    BOOLEAN     NOT NULL DEFAULT TRUE,
    share_stock              BOOLEAN     NOT NULL DEFAULT TRUE,
    share_targets            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_trade_link_uid (uid),
    INDEX idx_trade_link_brand (brand_workspace_id),
    INDEX idx_trade_link_distributor (distributor_workspace_id),
    INDEX idx_trade_link_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE network_retailers
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    uid             VARCHAR(40) NOT NULL,
    link_uid        VARCHAR(40) NOT NULL,
    customer_uid    VARCHAR(40) NOT NULL,
    outlet_code     VARCHAR(60),
    identified_name VARCHAR(200),
    pincode         VARCHAR(12),
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_network_retailer_uid (uid),
    INDEX idx_network_retailer_link (link_uid),
    INDEX idx_network_retailer_customer (customer_uid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE network_brands
(
    id                            BIGINT      NOT NULL AUTO_INCREMENT,
    uid                           VARCHAR(40) NOT NULL,
    link_uid                      VARCHAR(40) NOT NULL,
    distributor_product_brand_uid VARCHAR(40) NOT NULL,
    brand_workspace_id            VARCHAR(40) NOT NULL,
    status                        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_network_brand_uid (uid),
    INDEX idx_network_brand_link (link_uid),
    INDEX idx_network_brand_label (distributor_product_brand_uid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE network_products
(
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    uid                     VARCHAR(40) NOT NULL,
    link_uid                VARCHAR(40) NOT NULL,
    distributor_product_uid VARCHAR(40) NOT NULL,
    brand_product_uid       VARCHAR(40),
    brand_sku_code          VARCHAR(80),
    match_source            VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    status                  VARCHAR(20) NOT NULL DEFAULT 'SUGGESTED',
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_network_product_uid (uid),
    INDEX idx_network_product_link (link_uid),
    INDEX idx_network_product_distributor (distributor_product_uid),
    INDEX idx_network_product_brand (brand_product_uid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE scheme_publications
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    uid        VARCHAR(40) NOT NULL,
    link_uid   VARCHAR(40) NOT NULL,
    scheme_ref VARCHAR(40) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_scheme_publication_uid (uid),
    INDEX idx_scheme_publication_link (link_uid),
    INDEX idx_scheme_publication_scheme (scheme_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE primary_order_links
(
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    uid                      VARCHAR(40) NOT NULL,
    brand_workspace_id       VARCHAR(40) NOT NULL,
    distributor_workspace_id VARCHAR(40) NOT NULL,
    brand_order_uid          VARCHAR(40) NOT NULL,
    distributor_order_uid    VARCHAR(40),
    status                   VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    created_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_primary_order_uid (uid),
    INDEX idx_primary_order_brand (brand_workspace_id),
    INDEX idx_primary_order_distributor (distributor_workspace_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
