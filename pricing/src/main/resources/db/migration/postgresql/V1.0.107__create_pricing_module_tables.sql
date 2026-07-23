-- Pricing Module Database Migration (PostgreSQL)
-- Description: Channel/segment-aware price lists, tiered items, and reusable geo-zones.
-- Dependencies: V1.0.0__create_core_tables.sql

-- =====================================================
-- Price List
-- =====================================================
CREATE TABLE price_list (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
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
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_price_list_owner ON price_list(owner_id);
CREATE INDEX idx_price_list_channel ON price_list(channel);
CREATE INDEX idx_price_list_customer_group ON price_list(customer_group_id);
CREATE INDEX idx_price_list_brand ON price_list(brand_id);

COMMENT ON TABLE price_list IS 'Workspace-scoped channel/segment-aware price lists';

-- =====================================================
-- Price List Item
-- =====================================================
CREATE TABLE price_list_item (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    price_list_id VARCHAR(200) NOT NULL,
    product_id VARCHAR(200) NOT NULL,
    variant_sku VARCHAR(200),
    unit_price DECIMAL(19,4) NOT NULL DEFAULT 0,
    moq DECIMAL(19,4),
    tiers_json TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_price_list_item_owner ON price_list_item(owner_id);
CREATE INDEX idx_price_list_item_list ON price_list_item(price_list_id);
CREATE INDEX idx_price_list_item_product ON price_list_item(product_id);

COMMENT ON TABLE price_list_item IS 'Per-product/variant entries within a price list, with optional MOQ and quantity tiers';

-- =====================================================
-- Geo Zone
-- =====================================================
CREATE TABLE geo_zone (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(200) NOT NULL,
    members_json TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_geo_zone_owner ON geo_zone(owner_id);

COMMENT ON TABLE geo_zone IS 'Reusable named geography zones (pincodes/ranges/states) for price/offer targeting';
