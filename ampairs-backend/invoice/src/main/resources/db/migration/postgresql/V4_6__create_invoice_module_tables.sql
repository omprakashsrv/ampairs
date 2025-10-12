-- Invoice Module Database Migration Script (PostgreSQL)
-- Version: 4.6
-- Description: Create invoicing tables with item level tax capture
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_5__create_order_module_tables.sql

-- =====================================================
-- Invoice
-- =====================================================
-- Note: Customer references use VARCHAR(255) column lengths that differ from customer.uid (200),
--       so foreign keys are not enforced to maintain Hibernate validation compatibility.
CREATE TABLE invoice (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    invoice_number VARCHAR(255) NOT NULL UNIQUE,
    order_ref_id VARCHAR(255),
    invoice_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    from_customer_id VARCHAR(255) NOT NULL,
    from_customer_name VARCHAR(255) NOT NULL,
    to_customer_id VARCHAR(255) NOT NULL,
    to_customer_name VARCHAR(255) NOT NULL,
    place_of_supply VARCHAR(255) NOT NULL,
    from_customer_gst VARCHAR(30) NOT NULL,
    to_customer_gst VARCHAR(30) NOT NULL,
    total_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    base_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_items INT NOT NULL DEFAULT 0,
    total_quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    billing_address JSONB NOT NULL,
    shipping_address JSONB NOT NULL,
    discount JSONB,
    tax_info JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_invoice_order_ref FOREIGN KEY (order_ref_id) REFERENCES customer_order (ref_id) ON DELETE SET NULL
);

CREATE INDEX idx_invoice_order_ref ON invoice(order_ref_id);
CREATE INDEX idx_invoice_to_customer ON invoice(to_customer_id);
CREATE INDEX idx_invoice_owner_date ON invoice(owner_id, invoice_date);
COMMENT ON TABLE invoice IS 'Workspace invoices generated from orders';

-- =====================================================
-- Invoice Item
-- =====================================================
-- Note: invoice_id/product_id lengths follow entity definitions (VARCHAR(255)),
--       preventing FK constraints to invoice/product uid columns (VARCHAR(200)).
CREATE TABLE invoice_item (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    invoice_id VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    index_no INT NOT NULL DEFAULT 0,
    selling_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    product_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    mrp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dp DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    base_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_info JSONB,
    discount JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_item_invoice ON invoice_item(invoice_id);
CREATE INDEX idx_invoice_item_product ON invoice_item(product_id);
CREATE INDEX idx_invoice_item_owner ON invoice_item(owner_id);
COMMENT ON TABLE invoice_item IS 'Workspace invoice line items mirroring order items';
