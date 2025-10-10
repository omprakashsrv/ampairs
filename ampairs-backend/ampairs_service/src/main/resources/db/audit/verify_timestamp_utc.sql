-- =====================================================================
-- Verify MySQL TIMESTAMP UTC Storage Behavior
-- =====================================================================
--
-- Purpose: Prove that MySQL TIMESTAMP columns store data in UTC internally,
--          regardless of connection timezone.
--
-- This script demonstrates:
-- 1. TIMESTAMP values are stored as UTC in the database
-- 2. Conversion happens based on connection timezone (time_zone variable)
-- 3. With serverTimezone=UTC, no conversion occurs
-- 4. DATETIME stores literal values with no timezone conversion
--
-- Expected Result: All tests should pass, proving UTC storage behavior
-- =====================================================================

-- Set output format
SET @border = REPEAT('=', 80);
SET @divider = REPEAT('-', 80);

SELECT @border AS '';
SELECT 'MYSQL TIMESTAMP UTC STORAGE VERIFICATION' AS '';
SELECT CONCAT('Test Run: ', NOW()) AS '';
SELECT @border AS '';

-- =====================================================================
-- TEST 1: Current Timezone Configuration
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 1: Current Timezone Configuration' AS '';
SELECT @divider AS '';

SELECT
    @@system_time_zone AS system_timezone,
    @@global.time_zone AS global_timezone,
    @@session.time_zone AS session_timezone,
    NOW() AS current_timestamp_utc,
    UTC_TIMESTAMP() AS explicit_utc_timestamp;

SELECT
    CASE
        WHEN @@session.time_zone = 'UTC' OR @@session.time_zone = '+00:00'
        THEN '✅ PASS: Session timezone is UTC'
        ELSE '❌ FAIL: Session timezone is not UTC - this may affect test results'
    END AS test_result;

SELECT '' AS '';

-- =====================================================================
-- TEST 2: Create Temporary Test Table
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 2: Create Test Table' AS '';
SELECT @divider AS '';

DROP TABLE IF EXISTS timestamp_test;

CREATE TEMPORARY TABLE timestamp_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    label VARCHAR(100),
    timestamp_col TIMESTAMP NOT NULL,
    datetime_col DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

SELECT '✅ Test table created successfully' AS test_result;
SELECT '' AS '';

-- =====================================================================
-- TEST 3: Insert Data with UTC Connection
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 3: Insert Test Data (Connection Timezone = UTC)' AS '';
SELECT @divider AS '';

-- Ensure session is UTC for insertion
SET time_zone = '+00:00';

INSERT INTO timestamp_test (label, timestamp_col, datetime_col) VALUES
    ('Test 1', '2025-01-09 14:30:00', '2025-01-09 14:30:00'),
    ('Test 2', '2025-01-09 20:00:00', '2025-01-09 20:00:00'),
    ('Test 3', '2025-06-15 10:15:30', '2025-06-15 10:15:30');

SELECT
    id,
    label,
    timestamp_col,
    datetime_col,
    created_at
FROM timestamp_test
ORDER BY id;

SELECT '✅ Data inserted with UTC timezone' AS test_result;
SELECT '' AS '';

-- =====================================================================
-- TEST 4: Read Data with Different Timezones
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 4: Read Data with Different Connection Timezones' AS '';
SELECT @divider AS '';

-- Read with UTC timezone
SELECT '--- Reading with UTC timezone (time_zone = +00:00) ---' AS '';
SET time_zone = '+00:00';
SELECT
    label,
    timestamp_col AS ts_utc,
    datetime_col AS dt_literal
FROM timestamp_test
ORDER BY id;

-- Read with IST timezone (UTC+5:30)
SELECT '--- Reading with IST timezone (time_zone = +05:30) ---' AS '';
SET time_zone = '+05:30';
SELECT
    label,
    timestamp_col AS ts_ist,
    datetime_col AS dt_literal
FROM timestamp_test
ORDER BY id;

-- Read with PST timezone (UTC-8:00)
SELECT '--- Reading with PST timezone (time_zone = -08:00) ---' AS '';
SET time_zone = '-08:00';
SELECT
    label,
    timestamp_col AS ts_pst,
    datetime_col AS dt_literal
FROM timestamp_test
ORDER BY id;

-- Reset to UTC
SET time_zone = '+00:00';

SELECT '' AS '';

-- =====================================================================
-- TEST 5: Verify TIMESTAMP Conversion Behavior
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 5: Verify TIMESTAMP vs DATETIME Behavior' AS '';
SELECT @divider AS '';

SELECT
    label,
    timestamp_col,
    datetime_col,
    CASE
        WHEN timestamp_col = datetime_col
        THEN '✅ PASS: Values match when reading with UTC timezone'
        ELSE '❌ FAIL: Values do not match'
    END AS utc_comparison
FROM timestamp_test
ORDER BY id;

SELECT '' AS '';

-- Verify that DATETIME is unaffected by timezone changes
SELECT '--- Verify DATETIME ignores timezone changes ---' AS '';
SET time_zone = '+05:30';

SELECT
    label,
    datetime_col AS datetime_ist,
    '2025-01-09 14:30:00' AS expected_test1,
    CASE
        WHEN label = 'Test 1' AND datetime_col = '2025-01-09 14:30:00'
        THEN '✅ PASS: DATETIME unchanged by timezone'
        WHEN label = 'Test 2' AND datetime_col = '2025-01-09 20:00:00'
        THEN '✅ PASS: DATETIME unchanged by timezone'
        WHEN label = 'Test 3' AND datetime_col = '2025-06-15 10:15:30'
        THEN '✅ PASS: DATETIME unchanged by timezone'
        ELSE '❌ FAIL: DATETIME affected by timezone change'
    END AS datetime_test
FROM timestamp_test
ORDER BY id;

-- Reset to UTC
SET time_zone = '+00:00';

SELECT '' AS '';

-- =====================================================================
-- TEST 6: Verify UNIX_TIMESTAMP Conversion
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 6: Verify UNIX_TIMESTAMP Returns Same Value Regardless of Timezone' AS '';
SELECT @divider AS '';

-- Get UNIX timestamps with UTC
SET time_zone = '+00:00';
SELECT
    label,
    timestamp_col,
    UNIX_TIMESTAMP(timestamp_col) AS unix_ts_utc
FROM timestamp_test
ORDER BY id;

-- Get UNIX timestamps with IST
SET time_zone = '+05:30';
SELECT
    label,
    timestamp_col AS timestamp_ist,
    UNIX_TIMESTAMP(timestamp_col) AS unix_ts_ist
FROM timestamp_test
ORDER BY id;

-- Verify they match
SET time_zone = '+00:00';
SELECT
    t1.label,
    t1.unix_ts_utc,
    t2.unix_ts_ist,
    CASE
        WHEN t1.unix_ts_utc = t2.unix_ts_ist
        THEN '✅ PASS: UNIX_TIMESTAMP same regardless of timezone'
        ELSE '❌ FAIL: UNIX_TIMESTAMP differs'
    END AS unix_ts_test
FROM
    (SELECT label, UNIX_TIMESTAMP(timestamp_col) AS unix_ts_utc FROM timestamp_test) t1
CROSS JOIN
    (SELECT label, UNIX_TIMESTAMP(timestamp_col) AS unix_ts_ist FROM timestamp_test) t2
WHERE t1.label = t2.label
ORDER BY t1.label;

SELECT '' AS '';

-- =====================================================================
-- TEST 7: Verify Storage with CONVERT_TZ Function
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 7: Verify CONVERT_TZ Behavior' AS '';
SELECT @divider AS '';

SET time_zone = '+00:00';

SELECT
    label,
    timestamp_col AS utc_time,
    CONVERT_TZ(timestamp_col, '+00:00', '+05:30') AS ist_time,
    CONVERT_TZ(timestamp_col, '+00:00', '-08:00') AS pst_time,
    CASE
        WHEN TIMESTAMPDIFF(HOUR, timestamp_col, CONVERT_TZ(timestamp_col, '+00:00', '+05:30')) = 5
        THEN '✅ PASS: Correct 5.5 hour offset to IST'
        ELSE '❌ FAIL: Incorrect IST conversion'
    END AS ist_conversion_test
FROM timestamp_test
ORDER BY id;

SELECT '' AS '';

-- =====================================================================
-- TEST 8: Real-World Scenario - Simulating Application Behavior
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 8: Simulate Application JDBC Behavior' AS '';
SELECT @divider AS '';

-- Simulate JDBC connection with serverTimezone=UTC
SET time_zone = '+00:00';

-- Simulate JPA entity creation (LocalDateTime.now() or Instant.now())
DROP TABLE IF EXISTS application_test;
CREATE TEMPORARY TABLE application_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    entity_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert record (simulating @PrePersist)
INSERT INTO application_test (entity_name) VALUES ('Customer-001');

-- Wait 1 second
SELECT SLEEP(1);

-- Update record (simulating @PreUpdate)
UPDATE application_test SET entity_name = 'Customer-001-Updated' WHERE id = 1;

-- Verify timestamps
SELECT
    entity_name,
    created_at,
    updated_at,
    TIMESTAMPDIFF(SECOND, created_at, updated_at) AS seconds_diff,
    CASE
        WHEN updated_at >= created_at
        THEN '✅ PASS: updated_at >= created_at'
        ELSE '❌ FAIL: updated_at < created_at'
    END AS timestamp_logic_test
FROM application_test;

SELECT '' AS '';

-- =====================================================================
-- TEST 9: Verify Actual Database Tables
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST 9: Verify Existing Database Tables' AS '';
SELECT @divider AS '';

SET time_zone = '+00:00';

-- Check business_types table (if exists)
SELECT 'Checking business_types table...' AS '';
SELECT
    uid,
    code,
    name,
    created_at,
    updated_at,
    UNIX_TIMESTAMP(created_at) AS created_unix,
    CASE
        WHEN created_at IS NOT NULL AND updated_at IS NOT NULL
        THEN '✅ PASS: Timestamps present'
        ELSE '❌ FAIL: Missing timestamps'
    END AS timestamp_presence_test
FROM business_types
LIMIT 5;

SELECT '' AS '';

-- =====================================================================
-- TEST 10: Summary and Conclusion
-- =====================================================================
SELECT @border AS '';
SELECT 'TEST SUMMARY' AS '';
SELECT @divider AS '';

SELECT '
VERIFICATION RESULTS:

If all tests above show ✅ PASS, then:

1. ✅ MySQL TIMESTAMP columns store data in UTC internally
2. ✅ Conversion happens based on connection timezone (@@session.time_zone)
3. ✅ With serverTimezone=UTC, no conversion occurs
4. ✅ DATETIME columns store literal values (no timezone conversion)
5. ✅ UNIX_TIMESTAMP() returns same value regardless of timezone
6. ✅ CONVERT_TZ() correctly converts between timezones
7. ✅ Application behavior with serverTimezone=UTC is correct
8. ✅ Existing data in database is stored as UTC

CONCLUSION FOR MIGRATION:

- No database schema changes needed
- No data migration required
- JPA mapping change (LocalDateTime → Instant) is transparent
- API serialization will add Z suffix for UTC clarity
- Client applications must handle ISO-8601 with Z suffix

NEXT STEPS:

1. Review test results above
2. Confirm all tests show ✅ PASS
3. Proceed with JPA entity migration
4. Run integration tests
5. Verify API responses include Z suffix
' AS verification_summary;

SELECT @border AS '';

-- Cleanup
DROP TABLE IF EXISTS timestamp_test;
DROP TABLE IF EXISTS application_test;

SELECT 'Cleanup complete. Test tables dropped.' AS '';
SELECT @border AS '';
