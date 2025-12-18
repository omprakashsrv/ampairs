-- Unit Module Enhancement Migration
-- Version: V1.0.41
-- Description: Add category and description fields to unit table, extend field lengths
-- Date: 2025-01-18

-- Add missing fields to unit table
ALTER TABLE unit
ADD COLUMN description TEXT AFTER short_name,
ADD COLUMN category VARCHAR(50) AFTER description,
MODIFY COLUMN name VARCHAR(100) NOT NULL,
MODIFY COLUMN short_name VARCHAR(20) NOT NULL;

-- Add unique constraints for tenant isolation
ALTER TABLE unit
ADD UNIQUE KEY uk_unit_owner_name (owner_id, name),
ADD UNIQUE KEY uk_unit_owner_short_name (owner_id, short_name);

-- Add updated_at index for sync queries
CREATE INDEX idx_unit_updated_at ON unit(owner_id, updated_at);

-- Add active status index
CREATE INDEX idx_unit_active ON unit(owner_id, active);

-- Modify multiplier precision in unit_conversion table
ALTER TABLE unit_conversion
MODIFY COLUMN multiplier DECIMAL(20, 6) NOT NULL DEFAULT 1.0;

-- Add unique constraint for conversion combinations
ALTER TABLE unit_conversion
ADD UNIQUE KEY uk_conversion_product_units (owner_id, product_id, base_unit_id, derived_unit_id);

-- Add updated_at index for sync queries
CREATE INDEX idx_unit_conversion_updated_at ON unit_conversion(owner_id, updated_at);
