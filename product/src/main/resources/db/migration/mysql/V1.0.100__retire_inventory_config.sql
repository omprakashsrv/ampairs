-- Retire inventory_config (spec 014, T011) — MySQL
-- Inventory policy now lives in the central `setting` module (store_setting, module_code='inventory').
-- This migration backfills the 5 in-scope booleans into store_setting, then drops inventory_config.
-- Combined in one migration (single module) so backfill is guaranteed to run before the drop,
-- regardless of cross-module Flyway ordering.

INSERT INTO store_setting (uid, owner_id, module_code, setting_key, value, value_type, active, created_at, updated_at)
SELECT CONCAT('inv_cfg_', ic.owner_id, '_auto_deduct_on_order'), ic.owner_id, 'inventory', 'auto_deduct_on_order',
       CASE WHEN ic.auto_deduct_on_order = 1 THEN 'true' ELSE 'false' END, 'BOOLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_config ic
WHERE NOT EXISTS (SELECT 1 FROM store_setting s WHERE s.owner_id = ic.owner_id AND s.module_code = 'inventory' AND s.setting_key = 'auto_deduct_on_order');

INSERT INTO store_setting (uid, owner_id, module_code, setting_key, value, value_type, active, created_at, updated_at)
SELECT CONCAT('inv_cfg_', ic.owner_id, '_block_orders_when_out_of_stock'), ic.owner_id, 'inventory', 'block_orders_when_out_of_stock',
       CASE WHEN ic.block_orders_when_out_of_stock = 1 THEN 'true' ELSE 'false' END, 'BOOLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_config ic
WHERE NOT EXISTS (SELECT 1 FROM store_setting s WHERE s.owner_id = ic.owner_id AND s.module_code = 'inventory' AND s.setting_key = 'block_orders_when_out_of_stock');

INSERT INTO store_setting (uid, owner_id, module_code, setting_key, value, value_type, active, created_at, updated_at)
SELECT CONCAT('inv_cfg_', ic.owner_id, '_allow_negative_stock'), ic.owner_id, 'inventory', 'allow_negative_stock',
       CASE WHEN ic.allow_negative_stock = 1 THEN 'true' ELSE 'false' END, 'BOOLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_config ic
WHERE NOT EXISTS (SELECT 1 FROM store_setting s WHERE s.owner_id = ic.owner_id AND s.module_code = 'inventory' AND s.setting_key = 'allow_negative_stock');

INSERT INTO store_setting (uid, owner_id, module_code, setting_key, value, value_type, active, created_at, updated_at)
SELECT CONCAT('inv_cfg_', ic.owner_id, '_allow_manual_override'), ic.owner_id, 'inventory', 'allow_manual_override',
       CASE WHEN ic.allow_manual_override = 1 THEN 'true' ELSE 'false' END, 'BOOLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_config ic
WHERE NOT EXISTS (SELECT 1 FROM store_setting s WHERE s.owner_id = ic.owner_id AND s.module_code = 'inventory' AND s.setting_key = 'allow_manual_override');

INSERT INTO store_setting (uid, owner_id, module_code, setting_key, value, value_type, active, created_at, updated_at)
SELECT CONCAT('inv_cfg_', ic.owner_id, '_enable_low_stock_alerts'), ic.owner_id, 'inventory', 'enable_low_stock_alerts',
       CASE WHEN ic.enable_low_stock_alerts = 1 THEN 'true' ELSE 'false' END, 'BOOLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_config ic
WHERE NOT EXISTS (SELECT 1 FROM store_setting s WHERE s.owner_id = ic.owner_id AND s.module_code = 'inventory' AND s.setting_key = 'enable_low_stock_alerts');

DROP TABLE IF EXISTS inventory_config;
