package com.ampairs.core.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Utility functions for testing timezone-related functionality.
 *
 * Provides helper methods for:
 * - Verifying UTC timestamps in JSON
 * - Creating Instant objects for tests
 * - Comparing Instant values accounting for precision
 * - Converting between LocalDateTime and Instant
 *
 * Used across timezone migration tests to ensure consistent behavior.
 */
object TimezoneTestUtils {

    /**
     * Verify timestamp string ends with 'Z' (UTC indicator).
     *
     * Example: "2025-01-09T14:30:00Z" → passes
     *          "2025-01-09T14:30:00" → fails
     *
     * @param timestamp The timestamp string to verify
     * @throws AssertionError if timestamp doesn't end with 'Z'
     */
    fun assertIsUtcTimestamp(timestamp: String) {
        assert(timestamp.endsWith("Z")) {
            "Timestamp must end with 'Z' for UTC, but was: $timestamp"
        }
    }

    /**
     * Verify timestamp string matches ISO-8601 format with Z.
     *
     * Expected format: yyyy-MM-ddTHH:mm:ss.SSSZ or yyyy-MM-ddTHH:mm:ssZ
     *
     * @param timestamp The timestamp string to verify
     * @throws AssertionError if timestamp doesn't match ISO-8601 format
     */
    fun assertIsIso8601WithZ(timestamp: String) {
        val iso8601Pattern = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z")
        assert(iso8601Pattern.matches(timestamp)) {
            "Timestamp must match ISO-8601 format with Z, but was: $timestamp"
        }
    }

    /**
     * Create Instant from epoch milliseconds for testing.
     *
     * @param epochMilli Unix timestamp in milliseconds
     * @return Instant representing that moment in time
     */
    fun instantFromEpoch(epochMilli: Long): Instant {
        return Instant.ofEpochMilli(epochMilli)
    }

    /**
     * Create Instant from ISO-8601 string for testing.
     *
     * @param isoString ISO-8601 formatted string (e.g., "2025-01-09T14:30:00Z")
     * @return Instant parsed from string
     */
    fun instantFromIso(isoString: String): Instant {
        return Instant.parse(isoString)
    }

    /**
     * Verify two Instants are equal (accounting for precision).
     *
     * Compares epoch seconds, ignoring nanosecond differences that might occur
     * due to database storage precision (MySQL TIMESTAMP is second-precision).
     *
     * @param expected Expected Instant value
     * @param actual Actual Instant value
     * @throws AssertionError if Instants differ by more than 1 second
     */
    fun assertInstantsEqual(expected: Instant, actual: Instant) {
        val diffSeconds = Math.abs(expected.epochSecond - actual.epochSecond)
        assert(diffSeconds < 2) {
            "Instants not equal within 1 second: expected=$expected (${expected.epochSecond}), " +
                    "actual=$actual (${actual.epochSecond}), diff=${diffSeconds}s"
        }
    }

    /**
     * Verify Instant is recent (within last N seconds).
     *
     * Useful for verifying auto-generated timestamps like createdAt/updatedAt.
     *
     * @param instant The Instant to check
     * @param maxAgeSeconds Maximum age in seconds (default: 5)
     * @throws AssertionError if Instant is older than maxAgeSeconds
     */
    fun assertIsRecent(instant: Instant, maxAgeSeconds: Long = 5) {
        val now = Instant.now()
        val ageSeconds = now.epochSecond - instant.epochSecond
        assert(ageSeconds >= 0 && ageSeconds <= maxAgeSeconds) {
            "Instant is not recent: instant=$instant, now=$now, age=${ageSeconds}s (max ${maxAgeSeconds}s)"
        }
    }

    /**
     * Convert LocalDateTime (assumed UTC) to Instant.
     *
     * Helper for comparing old LocalDateTime values with new Instant values.
     *
     * @param localDateTime LocalDateTime to convert (assumed UTC timezone)
     * @return Instant representation
     */
    fun localDateTimeToInstant(localDateTime: LocalDateTime): Instant {
        return localDateTime.toInstant(ZoneOffset.UTC)
    }

    /**
     * Convert Instant to LocalDateTime in UTC.
     *
     * Helper for displaying Instant values in tests.
     *
     * @param instant Instant to convert
     * @return LocalDateTime in UTC timezone
     */
    fun instantToLocalDateTime(instant: Instant): LocalDateTime {
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
    }

    /**
     * Format Instant as ISO-8601 string with Z.
     *
     * @param instant Instant to format
     * @return ISO-8601 string (e.g., "2025-01-09T14:30:00Z")
     */
    fun formatIso8601(instant: Instant): String {
        return instant.toString()
    }

    /**
     * Create a test Instant representing a fixed moment.
     *
     * Useful for consistent test data.
     *
     * @return Instant representing 2025-01-09 14:30:00 UTC
     */
    fun testInstant(): Instant {
        return Instant.parse("2025-01-09T14:30:00Z")
    }

    /**
     * Verify JSON contains timestamp field with UTC format.
     *
     * Checks that JSON string contains a field with ISO-8601 timestamp ending in 'Z'.
     *
     * @param json JSON string to check
     * @param fieldName Name of timestamp field (e.g., "created_at", "timestamp")
     * @throws AssertionError if field not found or doesn't have UTC format
     */
    fun assertJsonHasUtcTimestamp(json: String, fieldName: String) {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*\"([^\"]+)\"")
        val match = pattern.find(json)

        assert(match != null) {
            "JSON does not contain field '$fieldName': $json"
        }

        val timestampValue = match!!.groupValues[1]
        assertIsIso8601WithZ(timestampValue)
    }
}
