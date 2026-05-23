-- Unit Module Migration (PostgreSQL)
-- Version: 1.0.52
-- Description: Add category and description columns missing from unit table

ALTER TABLE unit
    ADD COLUMN IF NOT EXISTS category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS description TEXT;
