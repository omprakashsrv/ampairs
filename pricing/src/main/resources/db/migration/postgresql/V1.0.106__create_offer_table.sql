-- Pricing Module Extension: Offers / Promotions (PostgreSQL)
-- Description: Channel/segment-targeted promotions applied on top of resolved prices.
-- Dependencies: V1.0.102__create_pricing_module_tables.sql

-- =====================================================
-- Offer (Promotion)
-- =====================================================
CREATE TABLE offer (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(200) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority INT NOT NULL DEFAULT 0,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    customer_group_id VARCHAR(200),
    customer_type VARCHAR(100),
    brand_id VARCHAR(200),
    category_id VARCHAR(200),
    geo_zone_id VARCHAR(200),
    condition_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    cart_min_minor BIGINT,
    quantity_min DOUBLE PRECISION,
    coupon_code VARCHAR(100),
    coupon_limit INT,
    reward_type VARCHAR(20) NOT NULL DEFAULT 'PERCENT',
    reward_percent DOUBLE PRECISION,
    reward_flat_minor BIGINT,
    reward_cap_minor BIGINT,
    bogo_buy_qty INT,
    bogo_get_qty INT,
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    exclusive BOOLEAN NOT NULL DEFAULT FALSE,
    per_customer_limit INT,
    total_limit INT,
    used_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_offer_owner ON offer(owner_id);
CREATE INDEX idx_offer_channel ON offer(channel);
CREATE INDEX idx_offer_brand ON offer(brand_id);
CREATE INDEX idx_offer_geo_zone ON offer(geo_zone_id);

COMMENT ON TABLE offer IS 'Workspace-scoped channel/segment-targeted promotions applied on top of resolved prices';
