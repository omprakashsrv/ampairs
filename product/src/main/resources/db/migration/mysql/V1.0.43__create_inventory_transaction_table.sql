-- ============================================================================
-- Flyway Migration: V1.0.43
-- Description: Create Inventory Transaction Table
-- Author: Claude Code
-- Date: 2025-12-20
-- Dependencies: V1.0.42 (warehouse, inventory_item tables)
-- ============================================================================

-- ============================================================================
-- Table: inventory_transaction
-- Description: All stock movements with complete audit trail
-- ============================================================================

CREATE TABLE inventory_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Transaction Identification
    transaction_number VARCHAR(50) NOT NULL UNIQUE,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_reason VARCHAR(50) NOT NULL,

    -- Inventory Item Reference
    inventory_item_id VARCHAR(200) NOT NULL,

    -- Warehouse/Location
    warehouse_id VARCHAR(200) NOT NULL,
    to_warehouse_id VARCHAR(200),  -- For transfers

    -- Quantities
    quantity DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    balance_after DECIMAL(15,3) DEFAULT 0.000,

    -- Pricing
    unit_cost DECIMAL(15,2) DEFAULT 0.00,
    total_cost DECIMAL(15,2) DEFAULT 0.00,

    -- Batch/Serial Tracking
    batch_id VARCHAR(200),
    serial_numbers JSON,

    -- References (Link to Source Documents)
    reference_type VARCHAR(50),
    reference_id VARCHAR(200),
    reference_number VARCHAR(100),

    -- Transaction Metadata
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(1000),
    performed_by VARCHAR(200),

    -- Extensible Attributes
    attributes JSON,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_transaction_uid (uid),
    UNIQUE INDEX uk_transaction_number (transaction_number),
    INDEX idx_inventory_transaction_owner_id (owner_id),
    INDEX idx_inventory_transaction_item_id (inventory_item_id),
    INDEX idx_inventory_transaction_warehouse_id (warehouse_id),
    INDEX idx_inventory_transaction_to_warehouse_id (to_warehouse_id),
    INDEX idx_inventory_transaction_date (transaction_date),
    INDEX idx_inventory_transaction_type (transaction_type),
    INDEX idx_inventory_transaction_reference (reference_type, reference_id),
    INDEX idx_inventory_transaction_batch_id (batch_id),

    -- Foreign Key Comments (not enforced to allow cross-module flexibility)
    -- inventory_item_id references inventory_item(uid)
    -- warehouse_id references warehouse(uid)
    -- to_warehouse_id references warehouse(uid)
    -- batch_id references inventory_batch(uid) [will be created in V1.0.44]

    CONSTRAINT chk_inventory_transaction_quantity_positive
        CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_transaction_type_valid
        CHECK (transaction_type IN ('STOCK_IN', 'STOCK_OUT', 'TRANSFER', 'ADJUSTMENT', 'COUNT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Inventory transactions with complete audit trail';

-- ============================================================================
-- Migration Complete
-- ============================================================================
-- Tables Created:
-- 1. inventory_transaction  - All stock movements
--
-- Next Migration: V1.0.44 (Inventory Batch and Serial Tracking)
-- ============================================================================
