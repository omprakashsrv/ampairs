-- ============================================================================
-- Flyway Migration: V1.0.42
-- Description: Create Inventory Module Tables (Warehouse, InventoryItem, InventoryConfig)
-- Author: Claude Code
-- Date: 2025-12-19
-- Dependencies: V1.0.40 (Product module with variant support)
-- ============================================================================

-- ============================================================================
-- Table: warehouse
-- Description: Storage locations for inventory (warehouses, stores, godowns)
-- ============================================================================

CREATE TABLE warehouse (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Warehouse Details
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL,
    warehouse_type VARCHAR(50) NOT NULL DEFAULT 'WAREHOUSE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,

    -- Contact Information
    phone VARCHAR(20),
    email VARCHAR(100),
    manager_name VARCHAR(100),

    -- Address (embedded fields)
    street VARCHAR(255),
    street2 VARCHAR(255),
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    attention VARCHAR(100),
    pincode VARCHAR(20),

    -- Additional Information
    description VARCHAR(500),
    attributes JSON,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_warehouse_uid (uid),
    UNIQUE INDEX uk_warehouse_code_owner (code, owner_id),
    INDEX idx_warehouse_owner_id (owner_id),
    INDEX idx_warehouse_code (code),
    INDEX idx_warehouse_is_active (is_active),
    INDEX idx_warehouse_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Warehouse and storage location master table';

-- ============================================================================
-- Table: inventory_item
-- Description: Inventory items with multi-location stock tracking
-- ============================================================================

CREATE TABLE inventory_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL,
    ref_id VARCHAR(255),

    -- Identification
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),

    -- Product Module Integration (Optional)
    product_id VARCHAR(200),
    product_variant_id VARCHAR(200),

    -- Location
    warehouse_id VARCHAR(200) NOT NULL,

    -- Stock Levels (using DECIMAL for precision)
    current_stock DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    reserved_stock DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    available_stock DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    reorder_level DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    max_stock_level DECIMAL(15,3) NOT NULL DEFAULT 0.000,

    -- Unit of Measurement
    unit_id VARCHAR(200),

    -- Pricing
    cost_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    mrp DECIMAL(15,2) NOT NULL DEFAULT 0.00,

    -- Tracking Flags
    batch_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    serial_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Extensible Attributes
    attributes JSON,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_item_uid (uid),
    UNIQUE INDEX uk_inventory_item_sku (sku, owner_id),
    UNIQUE INDEX uk_inventory_item_product_warehouse (product_id, product_variant_id, warehouse_id, owner_id),
    INDEX idx_inventory_item_owner_id (owner_id),
    INDEX idx_inventory_item_sku (sku),
    INDEX idx_inventory_item_product_id (product_id),
    INDEX idx_inventory_item_warehouse_id (warehouse_id),
    INDEX idx_inventory_item_is_active (is_active),
    INDEX idx_inventory_item_current_stock (current_stock),

    -- Foreign Key Comments (not enforced to allow cross-module flexibility)
    -- warehouse_id references warehouse(uid)
    -- product_id references product(uid) [optional]
    -- product_variant_id references product_variant(uid) [optional]
    -- unit_id references unit(uid) [optional]

    CONSTRAINT chk_inventory_item_stock_positive
        CHECK (current_stock >= 0 AND reserved_stock >= 0),
    CONSTRAINT chk_inventory_item_stock_consistency
        CHECK (available_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Inventory items with multi-location stock tracking';

-- ============================================================================
-- Table: inventory_config
-- Description: Tenant-level inventory configuration and policies
-- ============================================================================

CREATE TABLE inventory_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200) NOT NULL UNIQUE,
    ref_id VARCHAR(255),

    -- Auto Inventory Deduction (Order Integration)
    auto_deduct_on_order BOOLEAN NOT NULL DEFAULT FALSE,
    allow_manual_override BOOLEAN NOT NULL DEFAULT TRUE,
    block_orders_when_out_of_stock BOOLEAN NOT NULL DEFAULT FALSE,

    -- Stock Consumption Strategy
    stock_consumption_strategy VARCHAR(20) NOT NULL DEFAULT 'FIFO',

    -- Tracking Defaults
    enable_batch_tracking_by_default BOOLEAN NOT NULL DEFAULT FALSE,
    enable_serial_tracking_by_default BOOLEAN NOT NULL DEFAULT FALSE,
    enable_expiry_tracking_by_default BOOLEAN NOT NULL DEFAULT FALSE,

    -- Alerts & Notifications
    enable_low_stock_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    enable_expiry_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    expiry_alert_days INT NOT NULL DEFAULT 30,
    enable_overstock_alerts BOOLEAN NOT NULL DEFAULT FALSE,

    -- Stock Policies
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    require_approval_for_adjustments BOOLEAN NOT NULL DEFAULT FALSE,

    -- Multi-location Defaults
    default_warehouse_id VARCHAR(200),

    -- Ledger & Reporting
    auto_generate_daily_ledger BOOLEAN NOT NULL DEFAULT TRUE,
    ledger_generation_hour INT NOT NULL DEFAULT 1,

    -- Extensible Attributes
    attributes JSON,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_config_uid (uid),
    UNIQUE INDEX uk_inventory_config_owner (owner_id),
    INDEX idx_inventory_config_owner_id (owner_id),

    CONSTRAINT chk_inventory_config_expiry_days
        CHECK (expiry_alert_days >= 1 AND expiry_alert_days <= 365),
    CONSTRAINT chk_inventory_config_ledger_hour
        CHECK (ledger_generation_hour >= 0 AND ledger_generation_hour <= 23)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tenant-level inventory configuration and policies';

-- ============================================================================
-- Migration Complete
-- ============================================================================
-- Tables Created:
-- 1. warehouse           - Storage locations
-- 2. inventory_item      - Inventory items with multi-location tracking
-- 3. inventory_config    - Tenant configuration
--
-- Next Migration: V1.0.43 (Inventory Transactions)
-- ============================================================================
