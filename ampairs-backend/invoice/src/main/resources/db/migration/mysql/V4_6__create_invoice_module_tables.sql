-- Invoice Module Database Migration Script
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
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    invoice_number VARCHAR(255) NOT NULL,
    order_ref_id VARCHAR(255),
    invoice_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    from_customer_id VARCHAR(255) NOT NULL,
    from_customer_name VARCHAR(255) NOT NULL,
    to_customer_id VARCHAR(255) NOT NULL,
    to_customer_name VARCHAR(255) NOT NULL,
    place_of_supply VARCHAR(255) NOT NULL,
    from_customer_gst VARCHAR(30) NOT NULL,
    to_customer_gst VARCHAR(30) NOT NULL,
    total_cost DOUBLE NOT NULL DEFAULT 0.0,
    base_price DOUBLE NOT NULL DEFAULT 0.0,
    total_tax DOUBLE NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_items INT NOT NULL DEFAULT 0,
    total_quantity DOUBLE NOT NULL DEFAULT 0.0,
    billing_address JSON NOT NULL,
    shipping_address JSON NOT NULL,
    discount JSON,
    tax_info JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_invoice_uid (uid),
    UNIQUE INDEX uk_invoice_number (invoice_number),
    INDEX idx_invoice_order_ref (order_ref_id),
    INDEX idx_invoice_to_customer (to_customer_id),
    INDEX idx_invoice_owner_date (owner_id, invoice_date),

    CONSTRAINT fk_invoice_order_ref FOREIGN KEY (order_ref_id) REFERENCES customer_order (ref_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace invoices generated from orders';

-- =====================================================
-- Invoice Item
-- =====================================================
-- Note: invoice_id/product_id lengths follow entity definitions (VARCHAR(255)),
--       preventing FK constraints to invoice/product uid columns (VARCHAR(200)).
CREATE TABLE invoice_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    invoice_id VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(255) NOT NULL,
    quantity DOUBLE NOT NULL DEFAULT 0.0,
    index_no INT NOT NULL DEFAULT 0,
    selling_price DOUBLE NOT NULL DEFAULT 0.0,
    product_price DOUBLE NOT NULL DEFAULT 0.0,
    mrp DOUBLE NOT NULL DEFAULT 0.0,
    dp DOUBLE NOT NULL DEFAULT 0.0,
    total_cost DOUBLE NOT NULL DEFAULT 0.0,
    base_price DOUBLE NOT NULL DEFAULT 0.0,
    total_tax DOUBLE NOT NULL DEFAULT 0.0,
    tax_info JSON,
    discount JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_invoice_item_uid (uid),
    INDEX idx_invoice_item_invoice (invoice_id),
    INDEX idx_invoice_item_product (product_id),
    INDEX idx_invoice_item_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace invoice line items mirroring order items';
