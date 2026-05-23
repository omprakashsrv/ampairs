-- Product/Inventory Module Migration (PostgreSQL)
-- Version: 1.0.51
-- Description: Replace inventory_transaction table (old schema matched Inventory entity,
--              not InventoryTransaction entity) with the correct column set.

DROP TABLE IF EXISTS inventory_transaction;

CREATE TABLE inventory_transaction (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    owner_id VARCHAR(200),
    ref_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transaction_number VARCHAR(50) NOT NULL UNIQUE,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_reason VARCHAR(50) NOT NULL,
    inventory_item_id VARCHAR(200) NOT NULL,
    warehouse_id VARCHAR(200) NOT NULL,
    to_warehouse_id VARCHAR(200),
    quantity NUMERIC(15, 3) NOT NULL DEFAULT 0,
    balance_after NUMERIC(15, 3) NOT NULL DEFAULT 0,
    unit_cost NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_cost NUMERIC(15, 2) NOT NULL DEFAULT 0,
    batch_id VARCHAR(200),
    serial_numbers JSON,
    reference_type VARCHAR(50),
    reference_id VARCHAR(200),
    reference_number VARCHAR(100),
    transaction_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(1000),
    performed_by VARCHAR(200),
    attributes JSON
);

CREATE INDEX idx_inventory_txn_uid ON inventory_transaction(uid);
CREATE INDEX idx_inventory_txn_number ON inventory_transaction(transaction_number);
CREATE INDEX idx_inventory_txn_type ON inventory_transaction(transaction_type);
CREATE INDEX idx_inventory_txn_item ON inventory_transaction(inventory_item_id);
CREATE INDEX idx_inventory_txn_warehouse ON inventory_transaction(warehouse_id);
CREATE INDEX idx_inventory_txn_owner_id ON inventory_transaction(owner_id);
CREATE INDEX idx_inventory_txn_date ON inventory_transaction(transaction_date);
COMMENT ON TABLE inventory_transaction IS 'Audit trail of all stock movements: purchases, sales, transfers, adjustments';
