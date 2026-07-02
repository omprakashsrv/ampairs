-- Pricing Module Extension: Coupon Redemptions (MySQL)
-- Description: Tracks each use of a COUPON-condition offer; enforces per-customer + global caps.
-- Dependencies: V1.0.112__create_offer_table.sql

CREATE TABLE coupon_redemption (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    coupon_offer_uid VARCHAR(200) NOT NULL,
    coupon_code VARCHAR(100) NOT NULL,
    customer_id VARCHAR(200),
    order_ref VARCHAR(200) NOT NULL,
    redeemed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_coupon_redemption_uid (uid),
    UNIQUE INDEX uq_coupon_redemption_order (owner_id, coupon_offer_uid, order_ref),
    INDEX idx_coupon_redemption_offer (coupon_offer_uid),
    INDEX idx_coupon_redemption_customer (coupon_offer_uid, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Per-order coupon redemptions; atomic double-spend guard via the unique (owner, coupon, order) constraint';
