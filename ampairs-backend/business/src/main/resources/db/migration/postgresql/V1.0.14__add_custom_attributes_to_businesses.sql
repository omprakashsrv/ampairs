-- Add custom_attributes column to businesses table for flexible data storage
-- Migration: V1.0.13
-- Description: Add custom_attributes JSONB column to support arbitrary key-value pairs

ALTER TABLE businesses
ADD COLUMN custom_attributes JSONB NULL;

COMMENT ON COLUMN businesses.custom_attributes IS 'Custom attributes for flexible business data storage';
