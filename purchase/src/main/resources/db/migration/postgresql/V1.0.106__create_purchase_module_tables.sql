-- Purchase Module Database Migration Script (PostgreSQL)
-- Description: Create purchase document tables with item-level breakdown (buy side).
-- Mirrors the sales order tables; a RECEIVED purchase increases inventory.

CREATE TABLE purchase (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    purchase_number VARCHAR(255) NOT NULL,
    purchase_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    supplier_id VARCHAR(36),
    supplier_name VARCHAR(255),
    supplier_phone VARCHAR(20),
    supplier_gst VARCHAR(30) NOT NULL DEFAULT '',
    supplier_invoice_number VARCHAR(255),
    purchase_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_date TIMESTAMP(6),
    place_of_supply VARCHAR(255) NOT NULL DEFAULT '',
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    base_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    price_mode VARCHAR(20) NOT NULL DEFAULT 'TAX_EXCLUSIVE',
    overall_discount_mode VARCHAR(30) NOT NULL DEFAULT 'POST_TAX_REDUCTION',
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
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX purchase_ref_idx ON purchase(ref_id);
CREATE INDEX idx_purchase_supplier ON purchase(supplier_id);
CREATE INDEX idx_purchase_status ON purchase(status);
CREATE INDEX idx_purchase_owner_date ON purchase(owner_id, purchase_date);
COMMENT ON TABLE purchase IS 'Workspace purchase documents (buy side) with supplier and item breakdown';

-- =====================================================
-- Purchase Item
-- =====================================================
CREATE TABLE purchase_item (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    purchase_id VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    index_no INT NOT NULL DEFAULT 0,
    unit_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    line_total DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    purchase_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    product_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    mrp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    base_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    unit_id VARCHAR(255) NOT NULL DEFAULT '',
    base_quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    variant_sku VARCHAR(255),
    tax_info JSONB,
    discount JSONB,
    attributes JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_purchase_item_purchase ON purchase_item(purchase_id);
CREATE INDEX idx_purchase_item_product ON purchase_item(product_id);
CREATE INDEX idx_purchase_item_owner ON purchase_item(owner_id);
COMMENT ON TABLE purchase_item IS 'Workspace purchase line items with tax and discount breakdown';
