-- Migration: Add performance indexes for Customer module
-- Purpose: Improve query performance for frequently searched fields
-- Date: 2025-11-15
-- Database: MySQL

-- Customer table indexes for frequently queried fields
CREATE INDEX IF NOT EXISTS idx_customer_phone ON customer(phone);
CREATE INDEX IF NOT EXISTS idx_customer_email ON customer(email);
CREATE INDEX IF NOT EXISTS idx_customer_gst_number ON customer(gst_number);
CREATE INDEX IF NOT EXISTS idx_customer_updated_at ON customer(updated_at);
CREATE INDEX IF NOT EXISTS idx_customer_status ON customer(status);
CREATE INDEX IF NOT EXISTS idx_customer_type ON customer(customer_type);
CREATE INDEX IF NOT EXISTS idx_customer_group ON customer(customer_group);
CREATE INDEX IF NOT EXISTS idx_customer_city ON customer(city);
CREATE INDEX IF NOT EXISTS idx_customer_state ON customer(state);

-- Customer images table indexes (only add if not already exists)
CREATE INDEX IF NOT EXISTS idx_customer_image_display_order ON customer_image(display_order);
