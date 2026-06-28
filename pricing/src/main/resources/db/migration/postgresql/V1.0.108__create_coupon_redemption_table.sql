-- Pricing Module Extension: Coupon Redemptions (PostgreSQL)
-- Description: Tracks each use of a COUPON-condition offer; enforces per-customer + global caps.
-- Dependencies: V1.0.106__create_offer_table.sql

CREATE TABLE coupon_redemption (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    coupon_offer_uid VARCHAR(200) NOT NULL,
    coupon_code VARCHAR(100) NOT NULL,
    customer_id VARCHAR(200),
    order_ref VARCHAR(200) NOT NULL,
    redeemed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_coupon_redemption_order UNIQUE (owner_id, coupon_offer_uid, order_ref)
);

CREATE INDEX idx_coupon_redemption_offer ON coupon_redemption(coupon_offer_uid);
CREATE INDEX idx_coupon_redemption_customer ON coupon_redemption(coupon_offer_uid, customer_id);

COMMENT ON TABLE coupon_redemption IS 'Per-order coupon redemptions; atomic double-spend guard via the unique (owner, coupon, order) constraint';
