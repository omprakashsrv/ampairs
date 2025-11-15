-- Migration: Add performance indexes for Order module
-- Purpose: Improve query performance for frequently searched fields
-- Date: 2025-11-15
-- Database: MySQL

-- Order table indexes for frequently queried fields
CREATE INDEX IF NOT EXISTS idx_order_customer_id ON customer_order(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_order_number ON customer_order(order_number);
CREATE INDEX IF NOT EXISTS idx_order_order_date ON customer_order(order_date);
CREATE INDEX IF NOT EXISTS idx_order_delivery_date ON customer_order(delivery_date);
CREATE INDEX IF NOT EXISTS idx_order_status ON customer_order(status);
CREATE INDEX IF NOT EXISTS idx_order_updated_at ON customer_order(updated_at);
CREATE INDEX IF NOT EXISTS idx_order_payment_method ON customer_order(payment_method);
CREATE INDEX IF NOT EXISTS idx_order_customer_phone ON customer_order(customer_phone);
CREATE INDEX IF NOT EXISTS idx_order_customer_name ON customer_order(customer_name);
CREATE INDEX IF NOT EXISTS idx_order_total_amount ON customer_order(total_amount);

-- Order items table indexes
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product_id ON order_item(product_id);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_order_customer_status ON customer_order(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_order_status_date ON customer_order(status, order_date);
