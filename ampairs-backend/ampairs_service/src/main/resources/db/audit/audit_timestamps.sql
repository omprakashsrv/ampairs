-- =====================================================================
-- Timezone Support Migration - Database Timestamp Audit Script
-- =====================================================================
--
-- Purpose: Audit existing timestamp data before migrating to Instant (UTC)
--
-- Key Findings Expected:
-- 1. MySQL TIMESTAMP columns already store data in UTC internally
-- 2. No data migration needed - column type remains TIMESTAMP
-- 3. JPA migration (LocalDateTime → Instant) is transparent to database
--
-- Important MySQL Behavior:
-- - TIMESTAMP columns convert from connection timezone to UTC for storage
-- - On retrieval, converts from UTC to connection timezone
-- - With serverTimezone=UTC in JDBC URL, no conversion happens (already UTC)
--
-- Run this script BEFORE and AFTER migration to verify data integrity.
-- =====================================================================

-- Set output format for better readability
SET @border = REPEAT('=', 80);
SET @divider = REPEAT('-', 80);

SELECT @border AS '';
SELECT 'TIMEZONE SUPPORT MIGRATION - DATABASE AUDIT REPORT' AS '';
SELECT CONCAT('Generated: ', NOW()) AS '';
SELECT @border AS '';

-- =====================================================================
-- SECTION 1: Database Configuration
-- =====================================================================
SELECT @border AS '';
SELECT '1. DATABASE CONFIGURATION' AS '';
SELECT @divider AS '';

-- Check current timezone settings
SELECT
    'System Timezone' AS setting_name,
    @@system_time_zone AS current_value
UNION ALL
SELECT
    'Global Timezone' AS setting_name,
    @@global.time_zone AS current_value
UNION ALL
SELECT
    'Session Timezone' AS setting_name,
    @@session.time_zone AS current_value;

SELECT '' AS '';

-- =====================================================================
-- SECTION 2: Table and Column Inventory
-- =====================================================================
SELECT @border AS '';
SELECT '2. TIMESTAMP COLUMNS INVENTORY' AS '';
SELECT @divider AS '';

-- Find all tables with timestamp columns
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    EXTRA
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_SCHEMA = DATABASE()
    AND DATA_TYPE IN ('timestamp', 'datetime')
ORDER BY
    TABLE_NAME, COLUMN_NAME;

SELECT '' AS '';

-- Count of timestamp columns by table
SELECT
    TABLE_NAME,
    COUNT(*) as timestamp_column_count
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_SCHEMA = DATABASE()
    AND DATA_TYPE IN ('timestamp', 'datetime')
GROUP BY
    TABLE_NAME
ORDER BY
    timestamp_column_count DESC, TABLE_NAME;

SELECT '' AS '';

-- =====================================================================
-- SECTION 3: Data Range Analysis
-- =====================================================================
SELECT @border AS '';
SELECT '3. TIMESTAMP DATA RANGE ANALYSIS' AS '';
SELECT @divider AS '';

-- Business Types timestamps
SELECT
    'business_types' AS table_name,
    COUNT(*) AS total_records,
    MIN(created_at) AS earliest_created,
    MAX(created_at) AS latest_created,
    MIN(updated_at) AS earliest_updated,
    MAX(updated_at) AS latest_updated
FROM business_types
WHERE created_at IS NOT NULL OR updated_at IS NOT NULL;

-- HSN Codes timestamps
SELECT
    'hsn_codes' AS table_name,
    COUNT(*) AS total_records,
    MIN(created_at) AS earliest_created,
    MAX(created_at) AS latest_created,
    MIN(updated_at) AS earliest_updated,
    MAX(updated_at) AS latest_updated
FROM hsn_codes
WHERE created_at IS NOT NULL OR updated_at IS NOT NULL;

-- Tax Configurations timestamps
SELECT
    'tax_configurations' AS table_name,
    COUNT(*) AS total_records,
    MIN(created_at) AS earliest_created,
    MAX(created_at) AS latest_created,
    MIN(updated_at) AS earliest_updated,
    MAX(updated_at) AS latest_updated
FROM tax_configurations
WHERE created_at IS NOT NULL OR updated_at IS NOT NULL;

-- Tax Rates timestamps
SELECT
    'tax_rates' AS table_name,
    COUNT(*) AS total_records,
    MIN(created_at) AS earliest_created,
    MAX(created_at) AS latest_created,
    MIN(updated_at) AS earliest_updated,
    MAX(updated_at) AS latest_updated
FROM tax_rates
WHERE created_at IS NOT NULL OR updated_at IS NOT NULL;

-- Workspace Events timestamps
SELECT
    'workspace_events' AS table_name,
    COUNT(*) AS total_records,
    MIN(created_at) AS earliest_created,
    MAX(created_at) AS latest_created,
    MIN(updated_at) AS earliest_updated,
    MAX(updated_at) AS latest_updated
FROM workspace_events
WHERE created_at IS NOT NULL OR updated_at IS NOT NULL;

-- Device Sessions timestamps
SELECT
    'device_sessions' AS table_name,
    COUNT(*) AS total_records,
    MIN(last_heartbeat) AS earliest_heartbeat,
    MAX(last_heartbeat) AS latest_heartbeat,
    MIN(connected_at) AS earliest_connected,
    MAX(connected_at) AS latest_connected,
    MIN(disconnected_at) AS earliest_disconnected,
    MAX(disconnected_at) AS latest_disconnected,
    MIN(created_at) AS earliest_created,
    MAX(created_at) AS latest_created,
    MIN(updated_at) AS earliest_updated,
    MAX(updated_at) AS latest_updated
FROM device_sessions
WHERE created_at IS NOT NULL OR updated_at IS NOT NULL;

SELECT '' AS '';

-- =====================================================================
-- SECTION 4: Data Quality Checks
-- =====================================================================
SELECT @border AS '';
SELECT '4. DATA QUALITY CHECKS' AS '';
SELECT @divider AS '';

-- Check for NULL timestamps (should only be allowed in nullable columns)
SELECT
    'business_types' AS table_name,
    COUNT(*) AS records_with_null_created,
    'created_at' AS column_name
FROM business_types
WHERE created_at IS NULL
UNION ALL
SELECT
    'business_types' AS table_name,
    COUNT(*) AS records_with_null_updated,
    'updated_at' AS column_name
FROM business_types
WHERE updated_at IS NULL
UNION ALL
SELECT
    'hsn_codes' AS table_name,
    COUNT(*) AS records_with_null_created,
    'created_at' AS column_name
FROM hsn_codes
WHERE created_at IS NULL
UNION ALL
SELECT
    'hsn_codes' AS table_name,
    COUNT(*) AS records_with_null_updated,
    'updated_at' AS column_name
FROM hsn_codes
WHERE updated_at IS NULL
UNION ALL
SELECT
    'tax_configurations' AS table_name,
    COUNT(*) AS records_with_null_created,
    'created_at' AS column_name
FROM tax_configurations
WHERE created_at IS NULL
UNION ALL
SELECT
    'tax_configurations' AS table_name,
    COUNT(*) AS records_with_null_updated,
    'updated_at' AS column_name
FROM tax_configurations
WHERE updated_at IS NULL
UNION ALL
SELECT
    'tax_rates' AS table_name,
    COUNT(*) AS records_with_null_created,
    'created_at' AS column_name
FROM tax_rates
WHERE created_at IS NULL
UNION ALL
SELECT
    'tax_rates' AS table_name,
    COUNT(*) AS records_with_null_updated,
    'updated_at' AS column_name
FROM tax_rates
WHERE updated_at IS NULL
UNION ALL
SELECT
    'workspace_events' AS table_name,
    COUNT(*) AS records_with_null_created,
    'created_at' AS column_name
FROM workspace_events
WHERE created_at IS NULL
UNION ALL
SELECT
    'workspace_events' AS table_name,
    COUNT(*) AS records_with_null_updated,
    'updated_at' AS column_name
FROM workspace_events
WHERE updated_at IS NULL
UNION ALL
SELECT
    'device_sessions' AS table_name,
    COUNT(*) AS records_with_null_created,
    'created_at' AS column_name
FROM device_sessions
WHERE created_at IS NULL
UNION ALL
SELECT
    'device_sessions' AS table_name,
    COUNT(*) AS records_with_null_updated,
    'updated_at' AS column_name
FROM device_sessions
WHERE updated_at IS NULL;

SELECT '' AS '';

-- Check for future timestamps (data quality issue)
SELECT
    'business_types' AS table_name,
    COUNT(*) AS future_timestamp_count,
    'created_at' AS column_name
FROM business_types
WHERE created_at > NOW()
UNION ALL
SELECT
    'hsn_codes' AS table_name,
    COUNT(*) AS future_timestamp_count,
    'created_at' AS column_name
FROM hsn_codes
WHERE created_at > NOW()
UNION ALL
SELECT
    'tax_configurations' AS table_name,
    COUNT(*) AS future_timestamp_count,
    'created_at' AS column_name
FROM tax_configurations
WHERE created_at > NOW()
UNION ALL
SELECT
    'tax_rates' AS table_name,
    COUNT(*) AS future_timestamp_count,
    'created_at' AS column_name
FROM tax_rates
WHERE created_at > NOW()
UNION ALL
SELECT
    'workspace_events' AS table_name,
    COUNT(*) AS future_timestamp_count,
    'created_at' AS column_name
FROM workspace_events
WHERE created_at > NOW()
UNION ALL
SELECT
    'device_sessions' AS table_name,
    COUNT(*) AS future_timestamp_count,
    'created_at' AS column_name
FROM device_sessions
WHERE created_at > NOW();

SELECT '' AS '';

-- Check for timestamps where updated_at < created_at (logical error)
SELECT
    'business_types' AS table_name,
    COUNT(*) AS invalid_update_count
FROM business_types
WHERE updated_at < created_at
UNION ALL
SELECT
    'hsn_codes' AS table_name,
    COUNT(*) AS invalid_update_count
FROM hsn_codes
WHERE updated_at < created_at
UNION ALL
SELECT
    'tax_configurations' AS table_name,
    COUNT(*) AS invalid_update_count
FROM tax_configurations
WHERE updated_at < created_at
UNION ALL
SELECT
    'tax_rates' AS table_name,
    COUNT(*) AS invalid_update_count
FROM tax_rates
WHERE updated_at < created_at
UNION ALL
SELECT
    'workspace_events' AS table_name,
    COUNT(*) AS invalid_update_count
FROM workspace_events
WHERE updated_at < created_at
UNION ALL
SELECT
    'device_sessions' AS table_name,
    COUNT(*) AS invalid_update_count
FROM device_sessions
WHERE updated_at < created_at;

SELECT '' AS '';

-- =====================================================================
-- SECTION 5: Sample Data Verification
-- =====================================================================
SELECT @border AS '';
SELECT '5. SAMPLE DATA VERIFICATION' AS '';
SELECT @divider AS '';

-- Sample records from each table (first 5 sorted by created_at)
SELECT
    'business_types - Sample Records' AS section;
SELECT
    uid,
    code,
    name,
    created_at,
    updated_at,
    last_updated
FROM business_types
ORDER BY created_at DESC
LIMIT 5;

SELECT '' AS '';

SELECT
    'hsn_codes - Sample Records' AS section;
SELECT
    uid,
    hsn_code,
    description,
    created_at,
    updated_at,
    last_updated
FROM hsn_codes
ORDER BY created_at DESC
LIMIT 5;

SELECT '' AS '';

SELECT
    'tax_configurations - Sample Records' AS section;
SELECT
    uid,
    created_at,
    updated_at,
    last_updated
FROM tax_configurations
ORDER BY created_at DESC
LIMIT 5;

SELECT '' AS '';

SELECT
    'workspace_events - Sample Records' AS section;
SELECT
    uid,
    event_type,
    created_at,
    updated_at
FROM workspace_events
ORDER BY created_at DESC
LIMIT 5;

SELECT '' AS '';

SELECT
    'device_sessions - Sample Records' AS section;
SELECT
    uid,
    last_heartbeat,
    connected_at,
    disconnected_at,
    created_at,
    updated_at
FROM device_sessions
ORDER BY created_at DESC
LIMIT 5;

SELECT '' AS '';

-- =====================================================================
-- SECTION 6: Timezone Assumptions Documentation
-- =====================================================================
SELECT @border AS '';
SELECT '6. TIMEZONE ASSUMPTIONS' AS '';
SELECT @divider AS '';

SELECT '
KEY ASSUMPTIONS FOR MIGRATION:

1. MYSQL TIMESTAMP BEHAVIOR:
   - TIMESTAMP columns store UTC internally (since MySQL 4.1.3)
   - Conversion from connection timezone to UTC happens on INSERT
   - Conversion from UTC to connection timezone happens on SELECT
   - With serverTimezone=UTC in JDBC URL, no conversion occurs

2. CURRENT DATA STATE:
   - All existing timestamps assumed to be in server timezone
   - Server timezone should be verified (check @@system_time_zone)
   - Data inserted via JDBC with serverTimezone=UTC is already UTC

3. MIGRATION IMPACT:
   - No database schema changes needed
   - No data migration needed
   - JPA mapping changes: LocalDateTime → Instant
   - Transparent to database layer

4. POST-MIGRATION BEHAVIOR:
   - JPA reads TIMESTAMP as Instant (UTC)
   - JPA writes Instant to TIMESTAMP (UTC)
   - API responses use ISO-8601 with Z suffix
   - Client handles timezone conversion for display

5. VERIFICATION STEPS:
   - Compare timestamp values before/after migration
   - Verify no data corruption or timezone shifts
   - Check API responses use ISO-8601 format
   - Ensure client-side display uses correct local timezone
' AS documentation;

SELECT '' AS '';

-- =====================================================================
-- SECTION 7: Backup Verification Queries
-- =====================================================================
SELECT @border AS '';
SELECT '7. PRE-MIGRATION BACKUP CHECKSUMS' AS '';
SELECT @divider AS '';

-- Create checksums for data verification after migration
SELECT
    'business_types' AS table_name,
    COUNT(*) AS record_count,
    COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
    COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM business_types
UNION ALL
SELECT
    'hsn_codes' AS table_name,
    COUNT(*) AS record_count,
    COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
    COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM hsn_codes
UNION ALL
SELECT
    'tax_configurations' AS table_name,
    COUNT(*) AS record_count,
    COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
    COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM tax_configurations
UNION ALL
SELECT
    'tax_rates' AS table_name,
    COUNT(*) AS record_count,
    COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
    COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM tax_rates
UNION ALL
SELECT
    'workspace_events' AS table_name,
    COUNT(*) AS record_count,
    COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
    COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM workspace_events
UNION ALL
SELECT
    'device_sessions' AS table_name,
    COUNT(*) AS record_count,
    COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
    COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM device_sessions;

SELECT '' AS '';

-- =====================================================================
-- SECTION 8: Summary and Recommendations
-- =====================================================================
SELECT @border AS '';
SELECT '8. AUDIT SUMMARY' AS '';
SELECT @divider AS '';

SELECT
    COUNT(DISTINCT TABLE_NAME) AS total_tables_with_timestamps
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_SCHEMA = DATABASE()
    AND DATA_TYPE IN ('timestamp', 'datetime');

SELECT
    COUNT(*) AS total_timestamp_columns
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_SCHEMA = DATABASE()
    AND DATA_TYPE IN ('timestamp', 'datetime');

SELECT '
RECOMMENDATIONS:

1. Run this audit script BEFORE migration and save output
2. After migration, run again and compare outputs
3. Verify checksums match (Section 7)
4. Check for any new NULL values (Section 4)
5. Confirm no timezone shifts in sample data (Section 5)
6. Update documentation with actual server timezone
7. Monitor API responses for correct ISO-8601 format
8. Test client-side timezone conversion

NEXT STEPS:
- Proceed with JPA entity migration (LocalDateTime → Instant)
- Run integration tests
- Deploy to staging environment
- Verify API responses
- Update client applications
' AS recommendations;

SELECT @border AS '';
SELECT 'END OF AUDIT REPORT' AS '';
SELECT @border AS '';
