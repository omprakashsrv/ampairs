-- ============================================================================
-- Flyway Migration: V1.0.44
-- Description: Create Inventory Batch Table for Batch/Lot Tracking
-- Author: Claude Code
-- Date: 2025-12-20
-- Dependencies: V1.0.42 (inventory_item table), V1.0.43 (inventory_transaction table)
-- ============================================================================

-- ============================================================================
-- Table: inventory_batch
-- Description: Batch/lot tracking for inventory items with expiry management
-- ============================================================================

CREATE TABLE inventory_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Batch Identification
    batch_number VARCHAR(100) NOT NULL,
    lot_number VARCHAR(100),

    -- Inventory Item Reference
    inventory_item_id VARCHAR(200) NOT NULL,

    -- Warehouse/Location
    warehouse_id VARCHAR(200) NOT NULL,

    -- Stock Tracking
    total_quantity DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    available_quantity DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    reserved_quantity DECIMAL(15,3) NOT NULL DEFAULT 0.000,

    -- Dates
    manufacturing_date TIMESTAMP,
    expiry_date TIMESTAMP,
    received_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Supplier Information
    supplier_id VARCHAR(200),
    supplier_name VARCHAR(200),
    purchase_order_number VARCHAR(100),

    -- Cost
    cost_per_unit DECIMAL(15,2) DEFAULT 0.00,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_expired BOOLEAN NOT NULL DEFAULT FALSE,

    -- Extensible Attributes
    attributes JSON,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_batch_uid (uid),
    UNIQUE INDEX uk_batch_number_item_warehouse (batch_number, inventory_item_id, warehouse_id, owner_id),
    INDEX idx_inventory_batch_owner_id (owner_id),
    INDEX idx_inventory_batch_item_id (inventory_item_id),
    INDEX idx_inventory_batch_warehouse_id (warehouse_id),
    INDEX idx_inventory_batch_expiry_date (expiry_date),
    INDEX idx_inventory_batch_received_date (received_date),
    INDEX idx_inventory_batch_is_active (is_active),
    INDEX idx_inventory_batch_is_expired (is_expired),
    INDEX idx_inventory_batch_available_qty (available_quantity),

    -- Foreign Key Comments (not enforced to allow cross-module flexibility)
    -- inventory_item_id references inventory_item(uid)
    -- warehouse_id references warehouse(uid)
    -- supplier_id references supplier(uid) [if supplier module exists]

    CONSTRAINT chk_inventory_batch_quantities_positive
        CHECK (total_quantity >= 0 AND available_quantity >= 0 AND reserved_quantity >= 0),
    CONSTRAINT chk_inventory_batch_quantities_logical
        CHECK (available_quantity + reserved_quantity <= total_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Batch/lot tracking for inventory items with FIFO/FEFO support';

-- ============================================================================
-- Update inventory_transaction table to add foreign key index for batch_id
-- (Already created in V1.0.43 with batch_id column, this adds better indexing)
-- ============================================================================

-- The batch_id column already exists in inventory_transaction from V1.0.43
-- Just ensuring the index exists for performance
CREATE INDEX IF NOT EXISTS idx_inventory_transaction_batch_id
ON inventory_transaction(batch_id);

-- ============================================================================
-- Migration Complete
-- ============================================================================
-- Tables Created:
-- 1. inventory_batch  - Batch/lot tracking with expiry management
--
-- Features:
-- - Unique batch number per item per warehouse per tenant
-- - FIFO/FEFO/LIFO allocation support via received_date and expiry_date indexes
-- - Stock level tracking (total, available, reserved)
-- - Expiry date management with is_expired flag
-- - Supplier information tracking
-- - Cost per unit for weighted average calculations
--
-- Next Migration: V1.0.45 (Inventory Serial Tracking)
-- ============================================================================
