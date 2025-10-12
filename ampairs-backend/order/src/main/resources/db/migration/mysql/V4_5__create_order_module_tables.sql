-- Order Module Database Migration Script
-- Version: 4.5
-- Description: Create sales order tables with item level breakdown
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V4_4__create_product_module_tables.sql

-- Note: customer_id is stored as VARCHAR(36) (per entity) while customer.uid = VARCHAR(200),
--       therefore referential constraint is documented but not enforced to preserve schema validation.
CREATE TABLE customer_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
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
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_date TIMESTAMP NULL,
    from_customer_id VARCHAR(255) NOT NULL,
    from_customer_name VARCHAR(255) NOT NULL,
    to_customer_id VARCHAR(255) NOT NULL,
    to_customer_name VARCHAR(255) NOT NULL,
    place_of_supply VARCHAR(255) NOT NULL,
    from_customer_gst VARCHAR(30) NOT NULL,
    to_customer_gst VARCHAR(30) NOT NULL,
    subtotal DOUBLE NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE NOT NULL DEFAULT 0.0,
    tax_amount DOUBLE NOT NULL DEFAULT 0.0,
    total_amount DOUBLE NOT NULL DEFAULT 0.0,
    total_cost DOUBLE NOT NULL DEFAULT 0.0,
    base_price DOUBLE NOT NULL DEFAULT 0.0,
    total_tax DOUBLE NOT NULL DEFAULT 0.0,
    notes TEXT,
    internal_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_items INT NOT NULL DEFAULT 0,
    total_quantity DOUBLE NOT NULL DEFAULT 0.0,
    billing_address JSON NOT NULL,
    shipping_address JSON NOT NULL,
    discount JSON,
    tax_info JSON,
    attributes JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_order_uid (uid),
    UNIQUE INDEX order_ref_idx (ref_id),
    INDEX idx_order_customer (customer_id),
    INDEX idx_order_status (status),
    INDEX idx_order_owner_date (owner_id, order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace sales orders with billing and fulfilment details';

-- =====================================================
-- Order Item
-- =====================================================
-- Note: order_id/product_id columns use VARCHAR(255) lengths that differ from parent uid definitions.
--       Hibernate schema validation would fail if we shrink column length, so FK constraints are not enforced here.
CREATE TABLE order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    order_id VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(255) NOT NULL,
    quantity DOUBLE NOT NULL DEFAULT 0.0,
    index_no INT NOT NULL DEFAULT 0,
    unit_price DOUBLE NOT NULL DEFAULT 0.0,
    line_total DOUBLE NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE NOT NULL DEFAULT 0.0,
    selling_price DOUBLE NOT NULL DEFAULT 0.0,
    product_price DOUBLE NOT NULL DEFAULT 0.0,
    mrp DOUBLE NOT NULL DEFAULT 0.0,
    dp DOUBLE NOT NULL DEFAULT 0.0,
    total_cost DOUBLE NOT NULL DEFAULT 0.0,
    base_price DOUBLE NOT NULL DEFAULT 0.0,
    total_tax DOUBLE NOT NULL DEFAULT 0.0,
    tax_info JSON,
    discount JSON,
    attributes JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_updated BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_order_item_uid (uid),
    INDEX idx_order_item_order (order_id),
    INDEX idx_order_item_product (product_id),
    INDEX idx_order_item_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace order line items with tax and discount breakdown';
