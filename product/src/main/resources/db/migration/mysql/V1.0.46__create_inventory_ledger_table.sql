-- ============================================================================
-- Flyway Migration: V1.0.46
-- Description: Create Inventory Ledger Table for Daily Stock Aggregation
-- Author: Claude Code
-- Date: 2025-12-20
-- Dependencies: V1.0.42 (inventory_item table), V1.0.43 (inventory_transaction table)
-- ============================================================================

-- ============================================================================
-- Table: inventory_ledger
-- Description: Daily stock ledger for historical tracking and reporting
-- ============================================================================

CREATE TABLE inventory_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Date
    ledger_date TIMESTAMP NOT NULL,

    -- References
    inventory_item_id VARCHAR(200) NOT NULL,
    warehouse_id VARCHAR(200) NOT NULL,

    -- Opening Balance
    opening_stock DECIMAL(15,3) NOT NULL DEFAULT 0.000,

    -- Additions (Inflows)
    stock_in DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    transfer_in DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    adjustment_in DECIMAL(15,3) NOT NULL DEFAULT 0.000,

    -- Deductions (Outflows)
    stock_out DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    transfer_out DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    adjustment_out DECIMAL(15,3) NOT NULL DEFAULT 0.000,

    -- Closing Balance
    closing_stock DECIMAL(15,3) NOT NULL DEFAULT 0.000,

    -- Valuation
    average_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    closing_value DECIMAL(15,2) NOT NULL DEFAULT 0.00,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_ledger_uid (uid),
    UNIQUE INDEX uk_ledger_item_warehouse_date (inventory_item_id, warehouse_id, ledger_date, owner_id),
    INDEX idx_inventory_ledger_owner_id (owner_id),
    INDEX idx_inventory_ledger_item_id (inventory_item_id),
    INDEX idx_inventory_ledger_warehouse_id (warehouse_id),
    INDEX idx_inventory_ledger_date (ledger_date),
    INDEX idx_inventory_ledger_item_warehouse (inventory_item_id, warehouse_id),
    INDEX idx_inventory_ledger_warehouse_date (warehouse_id, ledger_date),

    -- Foreign Key Comments (not enforced to allow cross-module flexibility)
    -- inventory_item_id references inventory_item(uid)
    -- warehouse_id references warehouse(uid)

    CONSTRAINT chk_inventory_ledger_quantities_positive
        CHECK (
            opening_stock >= 0
            AND stock_in >= 0
            AND transfer_in >= 0
            AND adjustment_in >= 0
            AND stock_out >= 0
            AND transfer_out >= 0
            AND adjustment_out >= 0
            AND closing_stock >= 0
        ),
    CONSTRAINT chk_inventory_ledger_closing_balance
        CHECK (
            closing_stock = opening_stock
                          + stock_in
                          + transfer_in
                          + adjustment_in
                          - stock_out
                          - transfer_out
                          - adjustment_out
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Daily stock ledger for inventory tracking and reporting';

-- ============================================================================
-- Migration Complete
-- ============================================================================
-- Tables Created:
-- 1. inventory_ledger  - Daily stock aggregation with movements and balances
--
-- Features:
-- - One entry per item per warehouse per day
-- - Complete movement tracking (in/out/transfers/adjustments)
-- - Opening and closing balances
-- - Stock valuation (average cost method)
-- - Historical stock analysis support
-- - Unique constraint ensures single entry per item/warehouse/date
-- - Check constraint validates closing balance calculation
--
-- Ledger Entry Flow:
-- Opening Stock + Stock In + Transfer In + Adjustment In
-- - Stock Out - Transfer Out - Adjustment Out
-- = Closing Stock
--
-- Usage:
-- - Daily ledger generation via scheduled job
-- - Historical stock reports
-- - Stock movement analysis
-- - Warehouse valuation
-- - Audit trail for stock levels
--
-- Next Migration: V1.0.47 (Future enhancements)
-- ============================================================================
