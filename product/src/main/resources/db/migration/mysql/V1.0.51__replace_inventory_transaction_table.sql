-- MySQL counterpart of postgresql/V1.0.51: the original inventory_transaction (V1.0.43)
-- matched the Inventory entity, not InventoryTransaction — replace it with the correct
-- column set. Also adds warehouse.address_phone, which the PostgreSQL history gained via
-- its V1.0.50 table definitions but the MySQL V1.0.42 create predates.

DROP TABLE IF EXISTS inventory_transaction;

CREATE TABLE inventory_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    owner_id VARCHAR(200),
    ref_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    transaction_number VARCHAR(50) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_reason VARCHAR(50) NOT NULL,
    inventory_item_id VARCHAR(200) NOT NULL,
    warehouse_id VARCHAR(200) NOT NULL,
    to_warehouse_id VARCHAR(200),
    quantity DECIMAL(15,3) NOT NULL DEFAULT 0,
    balance_after DECIMAL(15,3) NOT NULL DEFAULT 0,
    unit_cost DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_cost DECIMAL(15,2) NOT NULL DEFAULT 0,
    batch_id VARCHAR(200),
    serial_numbers JSON,
    reference_type VARCHAR(50),
    reference_id VARCHAR(200),
    reference_number VARCHAR(100),
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(1000),
    performed_by VARCHAR(200),
    attributes JSON,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_inventory_txn_uid (uid),
    UNIQUE INDEX uq_inventory_txn_number (transaction_number),
    INDEX idx_inventory_txn_type (transaction_type),
    INDEX idx_inventory_txn_item (inventory_item_id),
    INDEX idx_inventory_txn_warehouse (warehouse_id),
    INDEX idx_inventory_txn_owner_id (owner_id),
    INDEX idx_inventory_txn_date (transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail of all stock movements: purchases, sales, transfers, adjustments';

ALTER TABLE warehouse ADD COLUMN address_phone VARCHAR(20);
