-- Effective-dated standard purchase cost (PostgreSQL)
-- Cost-side mirror of price_list_item effective dating (Tally "Standard Cost" / "Applicable From").
-- Dependencies: V1.0.0__create_core_tables.sql

CREATE TABLE product_standard_cost (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    product_id VARCHAR(200) NOT NULL,
    variant_sku VARCHAR(200),
    cost_price DECIMAL(15,2) NOT NULL DEFAULT 0,
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_standard_cost_owner ON product_standard_cost(owner_id);
CREATE INDEX idx_product_standard_cost_product ON product_standard_cost(product_id);

COMMENT ON TABLE product_standard_cost IS 'Effective-dated standard purchase cost per product/variant (Tally Applicable From)';
