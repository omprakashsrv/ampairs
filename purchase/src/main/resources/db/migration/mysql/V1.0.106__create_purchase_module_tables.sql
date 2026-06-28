-- Purchase Module Database Migration Script (MySQL)
-- Description: Create purchase document tables with item-level breakdown (buy side).
-- Mirrors the sales order tables; a RECEIVED purchase increases inventory.

CREATE TABLE purchase (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    purchase_number VARCHAR(255) NOT NULL,
    purchase_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    supplier_id VARCHAR(36),
    supplier_name VARCHAR(255),
    supplier_phone VARCHAR(20),
    supplier_gst VARCHAR(30) NOT NULL DEFAULT '',
    supplier_invoice_number VARCHAR(255),
    purchase_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_date TIMESTAMP NULL,
    place_of_supply VARCHAR(255) NOT NULL DEFAULT '',
    subtotal DOUBLE NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE NOT NULL DEFAULT 0.0,
    tax_amount DOUBLE NOT NULL DEFAULT 0.0,
    total_amount DOUBLE NOT NULL DEFAULT 0.0,
    total_cost DOUBLE NOT NULL DEFAULT 0.0,
    base_price DOUBLE NOT NULL DEFAULT 0.0,
    total_tax DOUBLE NOT NULL DEFAULT 0.0,
    price_mode VARCHAR(20) NOT NULL DEFAULT 'TAX_EXCLUSIVE',
    overall_discount_mode VARCHAR(30) NOT NULL DEFAULT 'POST_TAX_REDUCTION',
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

    PRIMARY KEY (id),
    UNIQUE INDEX idx_purchase_uid (uid),
    UNIQUE INDEX purchase_ref_idx (ref_id),
    INDEX idx_purchase_supplier (supplier_id),
    INDEX idx_purchase_status (status),
    INDEX idx_purchase_owner_date (owner_id, purchase_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace purchase documents (buy side) with supplier and item breakdown';

-- =====================================================
-- Purchase Item
-- =====================================================
CREATE TABLE purchase_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),
    purchase_id VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    tax_code VARCHAR(255) NOT NULL,
    quantity DOUBLE NOT NULL DEFAULT 0.0,
    index_no INT NOT NULL DEFAULT 0,
    unit_price DOUBLE NOT NULL DEFAULT 0.0,
    line_total DOUBLE NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE NOT NULL DEFAULT 0.0,
    purchase_price DOUBLE NOT NULL DEFAULT 0.0,
    product_price DOUBLE NOT NULL DEFAULT 0.0,
    mrp DOUBLE NOT NULL DEFAULT 0.0,
    dp DOUBLE NOT NULL DEFAULT 0.0,
    total_cost DOUBLE NOT NULL DEFAULT 0.0,
    base_price DOUBLE NOT NULL DEFAULT 0.0,
    total_tax DOUBLE NOT NULL DEFAULT 0.0,
    unit_id VARCHAR(255) NOT NULL DEFAULT '',
    base_quantity DOUBLE NOT NULL DEFAULT 0.0,
    variant_sku VARCHAR(255),
    tax_info JSON,
    discount JSON,
    attributes JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_purchase_item_uid (uid),
    INDEX idx_purchase_item_purchase (purchase_id),
    INDEX idx_purchase_item_product (product_id),
    INDEX idx_purchase_item_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace purchase line items with tax and discount breakdown';
