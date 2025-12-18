-- Unit Module Enhancement Migration (PostgreSQL)
-- Version: V1.0.41
-- Description: Add category and description fields to unit table, extend field lengths
-- Date: 2025-01-18

-- Add missing fields to unit table
ALTER TABLE unit
ADD COLUMN description TEXT,
ADD COLUMN category VARCHAR(50),
ALTER COLUMN name TYPE VARCHAR(100),
ALTER COLUMN short_name TYPE VARCHAR(20);

-- Add unique constraints for tenant isolation
ALTER TABLE unit
ADD CONSTRAINT uk_unit_owner_name UNIQUE (owner_id, name),
ADD CONSTRAINT uk_unit_owner_short_name UNIQUE (owner_id, short_name);

-- Add updated_at index for sync queries
CREATE INDEX idx_unit_updated_at ON unit(owner_id, updated_at);

-- Add active status index
CREATE INDEX idx_unit_active ON unit(owner_id, active);

-- Modify multiplier precision in unit_conversion table
ALTER TABLE unit_conversion
ALTER COLUMN multiplier TYPE DECIMAL(20, 6);

-- Add unique constraint for conversion combinations
ALTER TABLE unit_conversion
ADD CONSTRAINT uk_conversion_product_units UNIQUE (owner_id, product_id, base_unit_id, derived_unit_id);

-- Add updated_at index for sync queries
CREATE INDEX idx_unit_conversion_updated_at ON unit_conversion(owner_id, updated_at);
