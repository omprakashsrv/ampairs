-- ============================================================================
-- Flyway Migration: V1.0.45
-- Description: Create Inventory Serial Table for Serial Number Tracking
-- Author: Claude Code
-- Date: 2025-12-20
-- Dependencies: V1.0.42 (inventory_item table), V1.0.44 (inventory_batch table)
-- ============================================================================

-- ============================================================================
-- Table: inventory_serial
-- Description: Individual serial number tracking with lifecycle management
-- ============================================================================

CREATE TABLE inventory_serial (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Serial Number Identification
    serial_number VARCHAR(100) NOT NULL,

    -- Inventory Item Reference
    inventory_item_id VARCHAR(200) NOT NULL,

    -- Warehouse/Location
    warehouse_id VARCHAR(200) NOT NULL,

    -- Batch Association (Optional)
    batch_id VARCHAR(200),

    -- Status Lifecycle
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    -- Dates
    received_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sold_date TIMESTAMP,
    warranty_expiry_date TIMESTAMP,
    returned_date TIMESTAMP,

    -- References (Link to Sale/Order Documents)
    sold_reference_type VARCHAR(50),
    sold_reference_id VARCHAR(200),
    sold_reference_number VARCHAR(100),
    return_reference_type VARCHAR(50),
    return_reference_id VARCHAR(200),

    -- Customer Information (Optional)
    customer_id VARCHAR(200),
    customer_name VARCHAR(200),

    -- Pricing
    cost_price DECIMAL(15,2) DEFAULT 0.00,
    selling_price DECIMAL(15,2) DEFAULT 0.00,

    -- Additional Information
    notes VARCHAR(1000),
    attributes JSON,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_serial_uid (uid),
    UNIQUE INDEX uk_serial_number (serial_number, owner_id),
    INDEX idx_inventory_serial_owner_id (owner_id),
    INDEX idx_inventory_serial_item_id (inventory_item_id),
    INDEX idx_inventory_serial_warehouse_id (warehouse_id),
    INDEX idx_inventory_serial_batch_id (batch_id),
    INDEX idx_inventory_serial_status (status),
    INDEX idx_inventory_serial_customer_id (customer_id),
    INDEX idx_inventory_serial_received_date (received_date),
    INDEX idx_inventory_serial_sold_date (sold_date),
    INDEX idx_inventory_serial_warranty_expiry (warranty_expiry_date),
    INDEX idx_inventory_serial_sold_reference (sold_reference_type, sold_reference_id),
    INDEX idx_inventory_serial_return_reference (return_reference_type, return_reference_id),

    -- Foreign Key Comments (not enforced to allow cross-module flexibility)
    -- inventory_item_id references inventory_item(uid)
    -- warehouse_id references warehouse(uid)
    -- batch_id references inventory_batch(uid)
    -- customer_id references customer(uid) [if customer module exists]

    CONSTRAINT chk_inventory_serial_status
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'DAMAGED', 'RETURNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Serial number tracking for individual inventory units';

-- ============================================================================
-- Migration Complete
-- ============================================================================
-- Tables Created:
-- 1. inventory_serial  - Individual serial number tracking with lifecycle
--
-- Features:
-- - Unique serial number per tenant
-- - Status lifecycle management (AVAILABLE → RESERVED → SOLD)
-- - Batch association (optional)
-- - Customer tracking for sold serials
-- - Warranty expiry tracking
-- - Reference document linking (orders, invoices)
-- - Return handling
-- - FIFO allocation support via received_date index
--
-- Serial Lifecycle States:
-- - AVAILABLE: In stock, ready for sale
-- - RESERVED: Allocated to an order but not yet sold
-- - SOLD: Sold to customer
-- - DAMAGED: Damaged/defective
-- - RETURNED: Returned by customer
--
-- Next Migration: V1.0.46 (Inventory Ledger)
-- ============================================================================
