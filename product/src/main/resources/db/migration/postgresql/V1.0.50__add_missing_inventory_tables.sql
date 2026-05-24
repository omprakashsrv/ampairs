-- Product/Inventory Module Migration (PostgreSQL)
-- Version: 1.0.50
-- Description: Add missing inventory tables: warehouse, inventory_item, inventory_batch,
--              inventory_serial, inventory_ledger, inventory_config

-- =====================================================
-- Warehouse
-- =====================================================
CREATE TABLE warehouse (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200),
    ref_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL,
    warehouse_type VARCHAR(50) NOT NULL DEFAULT 'WAREHOUSE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    phone VARCHAR(20),
    email VARCHAR(100),
    manager_name VARCHAR(100),
    -- Embedded Address fields
    street VARCHAR(255),
    street2 VARCHAR(255),
    address VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(255),
    attention VARCHAR(255),
    pincode VARCHAR(255),
    address_phone VARCHAR(20),
    attributes JSON,
    description VARCHAR(500),

    CONSTRAINT uk_warehouse_code_owner UNIQUE (code, owner_id)
);

CREATE INDEX idx_warehouse_uid ON warehouse(uid);
CREATE INDEX idx_warehouse_code ON warehouse(code);
CREATE INDEX idx_warehouse_owner_id ON warehouse(owner_id);
CREATE INDEX idx_warehouse_is_active ON warehouse(is_active);
CREATE INDEX idx_warehouse_is_default ON warehouse(is_default);
COMMENT ON TABLE warehouse IS 'Physical warehouses, stores, and other storage locations';

-- =====================================================
-- Inventory Item
-- =====================================================
CREATE TABLE inventory_item (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200),
    ref_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    product_id VARCHAR(200),
    product_variant_id VARCHAR(200),
    warehouse_id VARCHAR(200) NOT NULL,
    current_stock NUMERIC(15, 3) NOT NULL DEFAULT 0,
    reserved_stock NUMERIC(15, 3) NOT NULL DEFAULT 0,
    available_stock NUMERIC(15, 3) NOT NULL DEFAULT 0,
    reorder_level NUMERIC(15, 3) NOT NULL DEFAULT 0,
    max_stock_level NUMERIC(15, 3) NOT NULL DEFAULT 0,
    unit_id VARCHAR(200),
    cost_price NUMERIC(15, 2) NOT NULL DEFAULT 0,
    selling_price NUMERIC(15, 2) NOT NULL DEFAULT 0,
    mrp NUMERIC(15, 2) NOT NULL DEFAULT 0,
    batch_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    serial_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    attributes JSON,

    CONSTRAINT uk_inventory_item_sku UNIQUE (sku, owner_id),
    CONSTRAINT uk_inventory_item_product_warehouse UNIQUE (product_id, product_variant_id, warehouse_id, owner_id)
);

CREATE INDEX idx_inventory_item_uid ON inventory_item(uid);
CREATE INDEX idx_inventory_item_sku ON inventory_item(sku);
CREATE INDEX idx_inventory_item_product_id ON inventory_item(product_id);
CREATE INDEX idx_inventory_item_warehouse_id ON inventory_item(warehouse_id);
CREATE INDEX idx_inventory_item_owner_id ON inventory_item(owner_id);
CREATE INDEX idx_inventory_item_is_active ON inventory_item(is_active);
COMMENT ON TABLE inventory_item IS 'Per-warehouse stock levels and tracking configuration for each SKU';

-- =====================================================
-- Inventory Batch
-- =====================================================
CREATE TABLE inventory_batch (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200),
    ref_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    batch_number VARCHAR(100) NOT NULL,
    lot_number VARCHAR(100),
    inventory_item_id VARCHAR(200) NOT NULL,
    warehouse_id VARCHAR(200) NOT NULL,
    total_quantity NUMERIC(15, 3) NOT NULL DEFAULT 0,
    available_quantity NUMERIC(15, 3) NOT NULL DEFAULT 0,
    reserved_quantity NUMERIC(15, 3) NOT NULL DEFAULT 0,
    manufacturing_date TIMESTAMPTZ,
    expiry_date TIMESTAMPTZ,
    received_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    supplier_id VARCHAR(200),
    supplier_name VARCHAR(200),
    purchase_order_number VARCHAR(100),
    cost_per_unit NUMERIC(15, 2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_expired BOOLEAN NOT NULL DEFAULT FALSE,
    attributes JSON,

    CONSTRAINT uk_batch_number_item_warehouse UNIQUE (batch_number, inventory_item_id, warehouse_id, owner_id)
);

CREATE INDEX idx_inventory_batch_uid ON inventory_batch(uid);
CREATE INDEX idx_inventory_batch_item ON inventory_batch(inventory_item_id);
CREATE INDEX idx_inventory_batch_warehouse ON inventory_batch(warehouse_id);
CREATE INDEX idx_inventory_batch_owner_id ON inventory_batch(owner_id);
CREATE INDEX idx_inventory_batch_expiry ON inventory_batch(expiry_date);
CREATE INDEX idx_inventory_batch_is_active ON inventory_batch(is_active);
COMMENT ON TABLE inventory_batch IS 'Batch/lot tracking for inventory items with FIFO/FEFO support';

-- =====================================================
-- Inventory Serial
-- =====================================================
CREATE TABLE inventory_serial (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200),
    ref_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    serial_number VARCHAR(100) NOT NULL,
    inventory_item_id VARCHAR(200) NOT NULL,
    warehouse_id VARCHAR(200) NOT NULL,
    batch_id VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    received_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sold_date TIMESTAMPTZ,
    warranty_expiry_date TIMESTAMPTZ,
    returned_date TIMESTAMPTZ,
    sold_reference_type VARCHAR(50),
    sold_reference_id VARCHAR(200),
    sold_reference_number VARCHAR(100),
    return_reference_type VARCHAR(50),
    return_reference_id VARCHAR(200),
    customer_id VARCHAR(200),
    customer_name VARCHAR(200),
    cost_price NUMERIC(15, 2) NOT NULL DEFAULT 0,
    selling_price NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes VARCHAR(1000),
    attributes JSON,

    CONSTRAINT uk_serial_number UNIQUE (serial_number, owner_id)
);

CREATE INDEX idx_inventory_serial_uid ON inventory_serial(uid);
CREATE INDEX idx_inventory_serial_number ON inventory_serial(serial_number);
CREATE INDEX idx_inventory_serial_item ON inventory_serial(inventory_item_id);
CREATE INDEX idx_inventory_serial_warehouse ON inventory_serial(warehouse_id);
CREATE INDEX idx_inventory_serial_owner_id ON inventory_serial(owner_id);
CREATE INDEX idx_inventory_serial_status ON inventory_serial(status);
COMMENT ON TABLE inventory_serial IS 'Serial number tracking with full lifecycle from receiving to sale';

-- =====================================================
-- Inventory Ledger
-- =====================================================
CREATE TABLE inventory_ledger (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200),
    ref_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ledger_date TIMESTAMPTZ NOT NULL,
    inventory_item_id VARCHAR(200) NOT NULL,
    warehouse_id VARCHAR(200) NOT NULL,
    opening_stock NUMERIC(15, 3) NOT NULL DEFAULT 0,
    stock_in NUMERIC(15, 3) NOT NULL DEFAULT 0,
    transfer_in NUMERIC(15, 3) NOT NULL DEFAULT 0,
    adjustment_in NUMERIC(15, 3) NOT NULL DEFAULT 0,
    stock_out NUMERIC(15, 3) NOT NULL DEFAULT 0,
    transfer_out NUMERIC(15, 3) NOT NULL DEFAULT 0,
    adjustment_out NUMERIC(15, 3) NOT NULL DEFAULT 0,
    closing_stock NUMERIC(15, 3) NOT NULL DEFAULT 0,
    average_cost NUMERIC(15, 2) NOT NULL DEFAULT 0,
    closing_value NUMERIC(15, 2) NOT NULL DEFAULT 0,

    CONSTRAINT uk_ledger_item_warehouse_date UNIQUE (inventory_item_id, warehouse_id, ledger_date, owner_id)
);

CREATE INDEX idx_inventory_ledger_uid ON inventory_ledger(uid);
CREATE INDEX idx_inventory_ledger_item ON inventory_ledger(inventory_item_id);
CREATE INDEX idx_inventory_ledger_warehouse ON inventory_ledger(warehouse_id);
CREATE INDEX idx_inventory_ledger_owner_id ON inventory_ledger(owner_id);
CREATE INDEX idx_inventory_ledger_date ON inventory_ledger(ledger_date);
COMMENT ON TABLE inventory_ledger IS 'Daily stock ledger — append-only audit trail of stock movements per item per warehouse';

-- =====================================================
-- Inventory Config
-- =====================================================
CREATE TABLE inventory_config (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200),
    ref_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auto_deduct_on_order BOOLEAN NOT NULL DEFAULT FALSE,
    allow_manual_override BOOLEAN NOT NULL DEFAULT TRUE,
    block_orders_when_out_of_stock BOOLEAN NOT NULL DEFAULT FALSE,
    stock_consumption_strategy VARCHAR(20) NOT NULL DEFAULT 'FIFO',
    enable_batch_tracking_by_default BOOLEAN NOT NULL DEFAULT FALSE,
    enable_serial_tracking_by_default BOOLEAN NOT NULL DEFAULT FALSE,
    enable_expiry_tracking_by_default BOOLEAN NOT NULL DEFAULT FALSE,
    enable_low_stock_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    enable_expiry_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    expiry_alert_days INT NOT NULL DEFAULT 30,
    enable_overstock_alerts BOOLEAN NOT NULL DEFAULT FALSE,
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    require_approval_for_adjustments BOOLEAN NOT NULL DEFAULT FALSE,
    default_warehouse_id VARCHAR(200),
    auto_generate_daily_ledger BOOLEAN NOT NULL DEFAULT TRUE,
    ledger_generation_hour INT NOT NULL DEFAULT 1,
    attributes JSON,

    CONSTRAINT uk_inventory_config_owner UNIQUE (owner_id)
);

CREATE INDEX idx_inventory_config_uid ON inventory_config(uid);
CREATE INDEX idx_inventory_config_owner_id ON inventory_config(owner_id);
COMMENT ON TABLE inventory_config IS 'Tenant-level inventory configuration — one row per workspace';