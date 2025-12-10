-- ============================================
-- Flyway Checksum Repair Script
-- ============================================
-- Purpose: Update the checksum for migration V1.0.38 to match the modified file
--
-- IMPORTANT: You MUST run this before starting your application
--            or you'll get: "Migration checksum mismatch for migration version 1.0.38"
--
-- How to run:
-- Option 1: IntelliJ Database Tool
--   1. Open Database tool window (View → Tool Windows → Database)
--   2. Connect to: localhost:5432/ampairs_prod
--   3. Open SQL console and run the UPDATE below
--
-- Option 2: Any PostgreSQL client (DBeaver, pgAdmin, etc.)
--   Connect to your database and execute the UPDATE statement
-- ============================================

-- Step 1: Verify the migration exists and check current checksum
SELECT
    version,
    description,
    checksum AS current_checksum,
    -138730348 AS new_checksum,
    installed_on
FROM flyway_schema_history
WHERE version = '1.0.38';

-- Step 2: Update the checksum to match the modified migration file
UPDATE flyway_schema_history
SET checksum = -138730348
WHERE version = '1.0.38';

-- Step 3: Verify the update was successful
SELECT
    version,
    description,
    checksum,
    'Checksum updated successfully!' AS status
FROM flyway_schema_history
WHERE version = '1.0.38';
