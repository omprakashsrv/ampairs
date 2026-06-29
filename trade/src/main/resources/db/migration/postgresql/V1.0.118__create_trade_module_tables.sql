-- Trade Module Migration (PostgreSQL)
-- Version: 1.0.118
-- Description: Cross-tenant network & consent edge — networks, links (+ consent scope), retailers,
--              Hop-A/Hop-B product attribution, scheme publications, primary-order handshake.
--              These are BaseDomain (no owner_id): the trust edge BETWEEN two workspaces.

CREATE TABLE trade_networks
(
    id                BIGSERIAL PRIMARY KEY,
    uid               VARCHAR(40)  NOT NULL UNIQUE,
    brand_workspace_id VARCHAR(40) NOT NULL,
    name              VARCHAR(150),
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_trade_network_brand ON trade_networks (brand_workspace_id);

CREATE TABLE trade_links
(
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(40)  NOT NULL UNIQUE,
    brand_workspace_id       VARCHAR(40)  NOT NULL,
    distributor_workspace_id VARCHAR(40)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'INVITED',
    retailer_visibility      VARCHAR(20)  NOT NULL DEFAULT 'CODED',
    share_secondary_sales    BOOLEAN      NOT NULL DEFAULT TRUE,
    share_stock              BOOLEAN      NOT NULL DEFAULT TRUE,
    share_targets            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_trade_link_brand ON trade_links (brand_workspace_id);
CREATE INDEX idx_trade_link_distributor ON trade_links (distributor_workspace_id);
CREATE INDEX idx_trade_link_status ON trade_links (status);

CREATE TABLE network_retailers
(
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(40)  NOT NULL UNIQUE,
    link_uid        VARCHAR(40)  NOT NULL,
    customer_uid    VARCHAR(40)  NOT NULL,
    outlet_code     VARCHAR(60),
    identified_name VARCHAR(200),
    pincode         VARCHAR(12),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_network_retailer_link ON network_retailers (link_uid);
CREATE INDEX idx_network_retailer_customer ON network_retailers (customer_uid);

CREATE TABLE network_brands
(
    id                            BIGSERIAL PRIMARY KEY,
    uid                           VARCHAR(40)  NOT NULL UNIQUE,
    link_uid                      VARCHAR(40)  NOT NULL,
    distributor_product_brand_uid VARCHAR(40)  NOT NULL,
    brand_workspace_id            VARCHAR(40)  NOT NULL,
    status                        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at                    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_network_brand_link ON network_brands (link_uid);
CREATE INDEX idx_network_brand_label ON network_brands (distributor_product_brand_uid);

CREATE TABLE network_products
(
    id                      BIGSERIAL PRIMARY KEY,
    uid                     VARCHAR(40)  NOT NULL UNIQUE,
    link_uid                VARCHAR(40)  NOT NULL,
    distributor_product_uid VARCHAR(40)  NOT NULL,
    brand_product_uid       VARCHAR(40),
    brand_sku_code          VARCHAR(80),
    match_source            VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'SUGGESTED',
    created_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_network_product_link ON network_products (link_uid);
CREATE INDEX idx_network_product_distributor ON network_products (distributor_product_uid);
CREATE INDEX idx_network_product_brand ON network_products (brand_product_uid);

CREATE TABLE scheme_publications
(
    id         BIGSERIAL PRIMARY KEY,
    uid        VARCHAR(40)  NOT NULL UNIQUE,
    link_uid   VARCHAR(40)  NOT NULL,
    scheme_ref VARCHAR(40)  NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_scheme_publication_link ON scheme_publications (link_uid);
CREATE INDEX idx_scheme_publication_scheme ON scheme_publications (scheme_ref);

CREATE TABLE primary_order_links
(
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(40)  NOT NULL UNIQUE,
    brand_workspace_id       VARCHAR(40)  NOT NULL,
    distributor_workspace_id VARCHAR(40)  NOT NULL,
    brand_order_uid          VARCHAR(40)  NOT NULL,
    distributor_order_uid    VARCHAR(40),
    status                   VARCHAR(20)  NOT NULL DEFAULT 'PLACED',
    created_at               TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_primary_order_brand ON primary_order_links (brand_workspace_id);
CREATE INDEX idx_primary_order_distributor ON primary_order_links (distributor_workspace_id);
