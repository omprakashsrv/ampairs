-- Order Module Database Migration Script (PostgreSQL)
-- Version: 4.5
-- Description: Create sales order tables with item level breakdown
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_4__create_product_module_tables.sql

-- Note: customer_id is stored as VARCHAR(36) (per entity) while customer.uid = VARCHAR(200),
--       therefore referential constraint is documented but not enforced to preserve schema validation.
CREATE TABLE customer_order (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    order_number VARCHAR(255) NOT NULL,
    order_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    customer_id VARCHAR(36),
    customer_name VARCHAR(255),
    customer_phone VARCHAR(20),
    is_walk_in BOOLEAN NOT NULL DEFAULT FALSE,
    payment_method VARCHAR(20) NOT NULL DEFAULT 'CASH',
    invoice_ref_id VARCHAR(255),
    order_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_date TIMESTAMP(6),
    from_customer_id VARCHAR(255) NOT NULL,
    from_customer_name VARCHAR(255) NOT NULL,
    to_customer_id VARCHAR(255) NOT NULL,
    to_customer_name VARCHAR(255) NOT NULL,
    place_of_supply VARCHAR(255) NOT NULL,
    from_customer_gst VARCHAR(30) NOT NULL,
    to_customer_gst VARCHAR(30) NOT NULL,
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    base_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    notes TEXT,
    internal_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_items INT NOT NULL DEFAULT 0,
    total_quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    billing_address JSONB NOT NULL,
    shipping_address JSONB NOT NULL,
    discount JSONB,
    tax_info JSONB,
    attributes JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX order_ref_idx ON customer_order(ref_id);
CREATE INDEX idx_order_customer ON customer_order(customer_id);
CREATE INDEX idx_order_status ON customer_order(status);
CREATE INDEX idx_order_owner_date ON customer_order(owner_id, order_date);
COMMENT ON TABLE customer_order IS 'Workspace sales orders with billing and fulfilment details';

-- =====================================================
-- Order Item
-- =====================================================
-- Note: order_id/product_id columns use VARCHAR(255) lengths that differ from parent uid definitions.
--       Hibernate schema validation would fail if we shrink column length, so FK constraints are not enforced here.
CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    order_id VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    index_no INT NOT NULL DEFAULT 0,
    unit_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    line_total DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    selling_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    product_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    mrp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    base_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_info JSONB,
    discount JSONB,
    attributes JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_order_item_order ON order_item(order_id);
CREATE INDEX idx_order_item_product ON order_item(product_id);
CREATE INDEX idx_order_item_owner ON order_item(owner_id);
COMMENT ON TABLE order_item IS 'Workspace order line items with tax and discount breakdown';
