-- Pricing Module Database Migration (MySQL)
-- Description: Channel/segment-aware price lists, tiered items, and reusable geo-zones.
-- Dependencies: V1.0.0__create_core_tables.sql

-- =====================================================
-- Price List
-- =====================================================
CREATE TABLE price_list (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(200) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    customer_group_id VARCHAR(200),
    customer_type VARCHAR(100),
    customer_id VARCHAR(200),
    brand_id VARCHAR(200),
    category_id VARCHAR(200),
    product_group_id VARCHAR(200),
    geo_zone_id VARCHAR(200),
    attribute_predicates_json TEXT,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    starts_at TIMESTAMP NULL DEFAULT NULL,
    ends_at TIMESTAMP NULL DEFAULT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_price_list_uid (uid),
    INDEX idx_price_list_owner (owner_id),
    INDEX idx_price_list_channel (channel),
    INDEX idx_price_list_customer_group (customer_group_id),
    INDEX idx_price_list_brand (brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-scoped channel/segment-aware price lists';

-- =====================================================
-- Price List Item
-- =====================================================
CREATE TABLE price_list_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    price_list_id VARCHAR(200) NOT NULL,
    product_id VARCHAR(200) NOT NULL,
    variant_sku VARCHAR(200),
    unit_price DECIMAL(19,4) NOT NULL DEFAULT 0,
    moq DECIMAL(19,4),
    tiers_json TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_price_list_item_uid (uid),
    INDEX idx_price_list_item_owner (owner_id),
    INDEX idx_price_list_item_list (price_list_id),
    INDEX idx_price_list_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Per-product/variant entries within a price list, with optional MOQ and quantity tiers';

-- =====================================================
-- Geo Zone
-- =====================================================
CREATE TABLE geo_zone (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(200) NOT NULL,
    members_json TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_geo_zone_uid (uid),
    INDEX idx_geo_zone_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Reusable named geography zones (pincodes/ranges/states) for price/offer targeting';
