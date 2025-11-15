-- Migration: Add performance indexes for Product module
-- Purpose: Improve query performance for frequently searched fields
-- Date: 2025-11-15
-- Database: MySQL

-- Product table indexes for frequently queried fields
CREATE INDEX IF NOT EXISTS idx_product_sku ON product(sku);
CREATE INDEX IF NOT EXISTS idx_product_code ON product(code);
CREATE INDEX IF NOT EXISTS idx_product_name ON product(name);
CREATE INDEX IF NOT EXISTS idx_product_updated_at ON product(updated_at);
CREATE INDEX IF NOT EXISTS idx_product_status ON product(status);
CREATE INDEX IF NOT EXISTS idx_product_category_id ON product(category_id);
CREATE INDEX IF NOT EXISTS idx_product_brand_id ON product(brand_id);
CREATE INDEX IF NOT EXISTS idx_product_group_id ON product(group_id);
CREATE INDEX IF NOT EXISTS idx_product_sub_category_id ON product(sub_category_id);
CREATE INDEX IF NOT EXISTS idx_product_unit_id ON product(unit_id);
CREATE INDEX IF NOT EXISTS idx_product_base_price ON product(base_price);

-- Product group table indexes
CREATE INDEX IF NOT EXISTS idx_product_group_name ON product_group(name);
CREATE INDEX IF NOT EXISTS idx_product_group_updated_at ON product_group(updated_at);

-- Product category table indexes
CREATE INDEX IF NOT EXISTS idx_product_category_name ON product_category(name);
CREATE INDEX IF NOT EXISTS idx_product_category_parent_id ON product_category(parent_id);

-- Product brand table indexes
CREATE INDEX IF NOT EXISTS idx_product_brand_name ON product_brand(name);

-- Product images table indexes
CREATE INDEX IF NOT EXISTS idx_product_image_product_uid ON product_image(product_uid);
