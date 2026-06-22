-- Retire the legacy flat `inventory` table (spec 014, T008) — MySQL
-- The new multi-field `inventory_item` model replaces it. This migration:
--   1. ensures a default warehouse exists for every owner that has legacy inventory rows
--      (inventory_item.warehouse_id is NOT NULL),
--   2. migrates legacy rows into inventory_item (description->name, prices, stock->current_stock),
--   3. drops the legacy `inventory` table.
-- Idempotent via LEFT JOIN ... IS NULL guards. Pre-launch data; rows already represented in
-- inventory_item (same product/owner) are skipped.

-- 1. Default warehouse for owners with legacy inventory but no default warehouse.
INSERT INTO warehouse (uid, owner_id, name, code, warehouse_type, is_active, is_default, created_at, updated_at)
SELECT CONCAT('whr_default_', sub.owner_id), sub.owner_id, 'Default Warehouse', 'DEFAULT', 'WAREHOUSE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT DISTINCT owner_id FROM inventory) sub
LEFT JOIN warehouse w ON w.owner_id = sub.owner_id AND w.is_default = 1
WHERE w.id IS NULL;

-- 2. Migrate legacy inventory rows into inventory_item.
INSERT INTO inventory_item (
    uid, owner_id, sku, name, product_id, warehouse_id,
    current_stock, available_stock, reserved_stock, reorder_level, max_stock_level,
    unit_id, cost_price, selling_price, mrp, is_active, created_at, updated_at
)
SELECT
    i.uid, i.owner_id, i.uid, i.description, i.product_id,
    (SELECT w.uid FROM warehouse w WHERE w.owner_id = i.owner_id AND w.is_default = 1 LIMIT 1),
    i.stock, i.stock, 0, 0, 0,
    i.unit_id, i.buying_price, i.selling_price, i.mrp, 1, i.created_at, i.updated_at
FROM inventory i
LEFT JOIN inventory_item ii ON ii.product_id = i.product_id AND ii.owner_id = i.owner_id
WHERE ii.id IS NULL;

-- 3. Drop the legacy tables. inventory_unit_conversion has an FK to inventory and its entity is
--    also retired, so drop the dependent table first, then inventory.
DROP TABLE IF EXISTS inventory_unit_conversion;
DROP TABLE IF EXISTS inventory;
