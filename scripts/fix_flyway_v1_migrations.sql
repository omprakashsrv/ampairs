-- Fix Flyway migration history after removing V1 tax module migrations
-- This script removes the V1.0.9 and V1.0.10 migration records from flyway_schema_history
-- Run this against the ampairs_prod database

-- Delete V1.0.10 (PostgreSQL V1 tax module migration)
DELETE FROM flyway_schema_history
WHERE version = '1.0.10'
AND description = 'create tax module tables';

-- Delete V1.0.9 (MySQL V1 tax module migration) if it exists
DELETE FROM flyway_schema_history
WHERE version = '1.0.9'
AND description = 'create tax module tables';

-- Verify the deletion
SELECT version, description, installed_on, success
FROM flyway_schema_history
WHERE version IN ('1.0.9', '1.0.10')
ORDER BY installed_rank;

-- Show current migration status
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;
