-- Pricing Module Extension: Offers / Promotions (MySQL)
-- Description: Channel/segment-targeted promotions applied on top of resolved prices.
-- Dependencies: V1.0.102__create_pricing_module_tables.sql

-- =====================================================
-- Offer (Promotion)
-- =====================================================
CREATE TABLE offer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    name VARCHAR(200) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority INT NOT NULL DEFAULT 0,
    starts_at TIMESTAMP NULL DEFAULT NULL,
    ends_at TIMESTAMP NULL DEFAULT NULL,
    customer_group_id VARCHAR(200),
    customer_type VARCHAR(100),
    brand_id VARCHAR(200),
    category_id VARCHAR(200),
    geo_zone_id VARCHAR(200),
    condition_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    cart_min_minor BIGINT,
    quantity_min DOUBLE,
    coupon_code VARCHAR(100),
    coupon_limit INT,
    reward_type VARCHAR(20) NOT NULL DEFAULT 'PERCENT',
    reward_percent DOUBLE,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_offer_uid (uid),
    INDEX idx_offer_owner (owner_id),
    INDEX idx_offer_channel (channel),
    INDEX idx_offer_brand (brand_id),
    INDEX idx_offer_geo_zone (geo_zone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-scoped channel/segment-targeted promotions applied on top of resolved prices';
