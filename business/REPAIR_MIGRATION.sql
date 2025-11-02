-- Repair Failed Flyway Migration for Business Module
-- Execute this script to clean up the failed migration attempt

-- Step 1: Check the flyway_schema_history table
SELECT * FROM flyway_schema_history WHERE version = '1.0.0';

-- Step 2: Remove the failed migration entry
DELETE FROM flyway_schema_history WHERE version = '1.0.0' AND success = 0;

-- Step 3: Drop the businesses table if it was partially created
DROP TABLE IF EXISTS businesses;

-- Step 4: Verify cleanup
SELECT * FROM flyway_schema_history WHERE version = '1.0.0';
SHOW TABLES LIKE 'businesses';

-- Now you can restart the application and Flyway will re-run the migration
